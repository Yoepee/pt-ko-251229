package com.blog.domain.battle.service

import com.blog.domain.battle.dto.request.AutoMatchRequest
import com.blog.domain.battle.dto.request.BattleInputRequest
import com.blog.domain.battle.dto.request.CreateRoomRequest
import com.blog.domain.battle.dto.response.AutoMatchResponse
import com.blog.domain.battle.dto.response.BattleMatchDetailResponse
import com.blog.domain.battle.dto.response.BattleRoomDetailResponse
import com.blog.domain.battle.dto.response.BattleRoomParticipantResponse
import com.blog.domain.battle.dto.response.BattleRoomSummaryResponse
import com.blog.domain.battle.dto.response.BattleCharacterResponse
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode
import com.blog.domain.battle.entity.BattleTeam
import com.blog.domain.battle.repository.*
import com.blog.global.common.PageResponse
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import jakarta.transaction.Transactional
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.time.ZoneId
import kotlin.math.pow

@Service
class BattleJooqService(
    private val seasonRepo: BattleSeasonJooqRepository,
    private val characterRepo: BattleCharacterJooqRepository,
    private val matchRepo: BattleMatchJooqRepository,
    private val partRepo: BattleParticipantJooqRepository,
    private val resultRepo: BattleResultJooqRepository,
    private val ratingRepo: BattleRatingJooqRepository,
    private val historyRepo: BattleRatingHistoryJooqRepository,
    private val battleRedis: BattleRedisPort
) {

    private val DURATION_MS = 30_000L
    private val MAX_PLAYERS = 2

    // -------------------------
    // Match Create / Join
    // -------------------------

    @Transactional
    fun createRankedMatch(userId: Long, mode: BattleMode): Long {
        val seasonId = seasonRepo.findActiveSeasonId()
            ?: throw ApiException(ErrorCode.NOT_FOUND) // 또는 BATTLE_SEASON_NOT_FOUND

        val focusLane = if (mode.name.contains("1LANE")) 1 else null

        return matchRepo.insertMatch(
            seasonId = seasonId,
            matchType = BattleMatchType.RANKED,
            mode = mode,
            createdByUserId = userId,
            focusLane = focusLane
        )
    }

    @Transactional
    fun createCustomRoom(userId: Long, req: CreateRoomRequest): Long {
        val seasonId = seasonRepo.findActiveSeasonId() ?: throw ApiException(ErrorCode.NOT_FOUND)
        val focusLane = if (req.mode.name.contains("1LANE")) 1 else null

        val matchId = matchRepo.insertMatch(
            seasonId = seasonId,
            matchType = BattleMatchType.CUSTOM,
            mode = req.mode,
            createdByUserId = userId,
            focusLane = focusLane
        )

        joinAs(matchId, userId, preferTeam = BattleTeam.A, characterId = req.characterId)
        return matchId
    }

    @Transactional
    fun leaveMatch(userId: Long, matchId: Long) {
        val status = matchRepo.findMatchStatus(matchId) ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.WAITING) throw ApiException(ErrorCode.BATTLE_MATCH_NOT_LEAVABLE)

        // ✅ 멱등: active 참가자가 아니면 그냥 종료
        if (!partRepo.existsActiveParticipant(matchId, userId)) return

        // ✅ left_at 찍기 (이미 left면 0)
        val affected = partRepo.markLeft(matchId, userId)
        if (affected == 0) return

        val remain = partRepo.countActiveParticipants(matchId)
        if (remain == 0L) {
            matchRepo.cancelIfWaiting(matchId)
            return
        }

        val owner = matchRepo.findOwnerUserId(matchId)
        if (owner == userId) {
            val newOwner = partRepo.findNextOwnerUserId(matchId)
                ?: run {
                    // 이론상 remain>0이면 null이 아니지만 방어
                    matchRepo.cancelIfWaiting(matchId)
                    return
                }
            matchRepo.updateOwner(matchId, newOwner)
        }
    }

    // -------------------------
    // Auto Match
    // -------------------------

    @Transactional
    fun autoMatch(userId: Long, req: AutoMatchRequest): AutoMatchResponse {
        val seasonId = seasonRepo.findActiveSeasonId()
            ?: throw ApiException(ErrorCode.NOT_FOUND) // 또는 BATTLE_SEASON_NOT_FOUND

        val joinableMatchId = matchRepo.lockJoinableMatchId(req.matchType, req.mode, MAX_PLAYERS)

        return if (joinableMatchId != null) {
            val team = joinAs(joinableMatchId, userId, preferTeam = BattleTeam.B, characterId = req.characterId)
            val count = partRepo.countActiveParticipants(joinableMatchId)

            if (count >= MAX_PLAYERS.toLong()) {
                matchRepo.updateMatchToRunning(joinableMatchId)
                scheduleMatchFinish(joinableMatchId)
                AutoMatchResponse(joinableMatchId, BattleMatchStatus.RUNNING, team)
            } else {
                AutoMatchResponse(joinableMatchId, BattleMatchStatus.WAITING, team)
            }
        } else {
            val focusLane = if (req.mode.name.contains("1LANE")) 1 else null

            val matchId = matchRepo.insertMatch(
                seasonId = seasonId,
                matchType = req.matchType,
                mode = req.mode,
                createdByUserId = userId,
                focusLane = focusLane
            )

            val team = joinAs(matchId, userId, preferTeam = BattleTeam.A, characterId = req.characterId)
            AutoMatchResponse(matchId, BattleMatchStatus.WAITING, team)
        }
    }

    private fun joinAs(matchId: Long, userId: Long, preferTeam: BattleTeam, characterId: Long?): BattleTeam {
        // ✅ 이미 참가했으면 실제 팀을 반환
        partRepo.findActiveTeam(matchId, userId)?.let { return it }

        val count = partRepo.countActiveParticipants(matchId)
        if (count >= MAX_PLAYERS) throw ApiException(ErrorCode.BATTLE_MATCH_FULL)

        val cid = characterId ?: characterRepo.findDefaultCharacterId()
        ?: throw ApiException(ErrorCode.NOT_FOUND)
        val cver = characterRepo.findCharacterVersionNo(cid)

        val team = when (count) {
            0L -> BattleTeam.A
            1L -> BattleTeam.B
            else -> throw ApiException(ErrorCode.BATTLE_MATCH_FULL)
        }

        partRepo.insertParticipant(matchId, userId, team, cid, cver)
        return team
    }

    private fun scheduleMatchFinish(matchId: Long) {
        val endsAt = System.currentTimeMillis() + DURATION_MS
        battleRedis.scheduleFinish(matchId, endsAt)
    }

    // -------------------------
    // Input / Detail
    // -------------------------

    @Transactional
    fun submitInput(userId: Long, matchId: Long, req: BattleInputRequest) {
        // match 상태 확인
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.RUNNING) throw ApiException(ErrorCode.BATTLE_MATCH_NOT_RUNNING)

        // 참가자/팀 확인
        val team = partRepo.findActiveTeam(matchId, userId)
            ?: throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        // Redis 누적 + rate limit
        val ok = battleRedis.submitInput(matchId, userId, req.lane, req.power, team)
        if (!ok) throw ApiException(ErrorCode.BATTLE_RATE_LIMIT_EXCEEDED)
    }

    @Transactional
    fun getMatchDetail(userId: Long, matchId: Long): BattleMatchDetailResponse {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        val myTeam = partRepo.findActiveTeam(matchId, userId)

        val snap = battleRedis.getLaneSnapshot(matchId)
        val endsAt = if (status == BattleMatchStatus.RUNNING) battleRedis.getEndsAt(matchId) else null

        // 응답은 UI용(팀 합산 표시가 필요하면 여기서 합산)
        return BattleMatchDetailResponse(
            matchId = matchId,
            status = status,
            myTeam = myTeam,
            lane0 = snap.lane0A + snap.lane0B,
            lane1 = snap.lane1A + snap.lane1B,
            lane2 = snap.lane2A + snap.lane2B,
            endsAtEpochMs = endsAt
        )
    }

    // -------------------------
    // Finish (Worker uses this)
    // -------------------------

    @Transactional
    fun finishByWorker(matchId: Long) {
        val info = matchRepo.getMatchInfo(matchId) ?: return

        // 이미 끝났으면 무시
        if (info.status != BattleMatchStatus.RUNNING) return

        val snap = battleRedis.getLaneSnapshot(matchId)

        val sumA = snap.lane0A + snap.lane1A + snap.lane2A
        val sumB = snap.lane0B + snap.lane1B + snap.lane2B

        val winner = when {
            sumA > sumB -> BattleTeam.A
            sumB > sumA -> BattleTeam.B
            else -> null // draw
        }

        finishMatch(
            matchId = matchId,
            winnerTeam = winner,                 // ✅ enum/nullable
            endReason = "TIMEOUT",
            lane0 = snap.lane0A + snap.lane0B,   // 결과 테이블이 합산형이면 이대로
            lane1 = snap.lane1A + snap.lane1B,
            lane2 = snap.lane2A + snap.lane2B,
            inputsA = snap.inputsA,
            inputsB = snap.inputsB,
            vsBot = false,
            extraStatsJson = """{"sumA":$sumA,"sumB":$sumB}"""
        )

        battleRedis.clearMatchKeys(matchId)
    }

    /**
     * 내부 finish (DB 결과 저장 + (조건이면) 레이팅 반영)
     * winnerTeam: A/B/null(draw)
     */
    @Transactional
    fun finishMatch(
        matchId: Long,
        winnerTeam: BattleTeam?, // ✅ enum으로
        endReason: String,
        lane0: Int, lane1: Int, lane2: Int,
        inputsA: Int, inputsB: Int,
        vsBot: Boolean,
        extraStatsJson: String = "{}"
    ) {
        val info = matchRepo.getMatchInfo(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (info.status != BattleMatchStatus.RUNNING) {
            // RUNNING 아니면 종료 불가(혹은 멱등 처리)
            return
        }

        // 원자적 FINISHED 전환
        val updated = matchRepo.finishIfRunning(matchId)
        if (updated == 0) return

        // 결과 저장
        val winnerStr = winnerTeam?.name ?: "DRAW"
        resultRepo.insertResult(
            matchId = matchId,
            winnerTeam = winnerStr,
            endReason = endReason,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = inputsA, inputsB = inputsB,
            extraStatsJson = extraStatsJson
        )

        // 레이팅 반영: ranked + 사람vs사람만
        if (info.matchType != BattleMatchType.RANKED || vsBot) return

        val p = partRepo.getTwoActivePlayers(matchId)
        applyEloAndHistory(
            seasonId = info.seasonId,
            matchId = matchId,
            userA = p.userA,
            userB = p.userB,
            winnerTeam = winnerTeam
        )
    }

    private fun applyEloAndHistory(
        seasonId: Long,
        matchId: Long,
        userA: Long,
        userB: Long,
        winnerTeam: BattleTeam?, // ✅ enum/nullable
    ) {
        // 락 순서 고정
        val (first, second) = if (userA <= userB) userA to userB else userB to userA

        ratingRepo.insertIfAbsent(seasonId, first)
        ratingRepo.insertIfAbsent(seasonId, second)

        val r1 = ratingRepo.lockRating(seasonId, first)!!
        val r2 = ratingRepo.lockRating(seasonId, second)!!

        val (ra, rb) = if (first == userA) r1 to r2 else r2 to r1
        val beforeA = ra.rating
        val beforeB = rb.rating

        val ea = expected(beforeA, beforeB)
        val eb = expected(beforeB, beforeA)

        val (sa, sb) = when (winnerTeam) {
            BattleTeam.A -> 1.0 to 0.0
            BattleTeam.B -> 0.0 to 1.0
            null -> 0.5 to 0.5
        }

        val k = 24
        val newA = (beforeA + k * (sa - ea)).toInt()
        val newB = (beforeB + k * (sb - eb)).toInt()

        val deltaA = newA - beforeA
        val deltaB = newB - beforeB

        val (winA, lossA, drawA) = when (winnerTeam) {
            BattleTeam.A -> Triple(1, 0, 0)
            BattleTeam.B -> Triple(0, 1, 0)
            null -> Triple(0, 0, 1)
        }
        val (winB, lossB, drawB) = when (winnerTeam) {
            BattleTeam.B -> Triple(1, 0, 0)
            BattleTeam.A -> Triple(0, 1, 0)
            null -> Triple(0, 0, 1)
        }

        ratingRepo.updateRating(seasonId, userA, newA, winA, lossA, drawA)
        ratingRepo.updateRating(seasonId, userB, newB, winB, lossB, drawB)

        historyRepo.insert(
            seasonId = seasonId,
            userId = userA,
            matchId = matchId,
            ratingBefore = beforeA,
            ratingAfter = newA,
            delta = deltaA,
            reason = "MATCH_RESULT",
            vsBot = false
        )
        historyRepo.insert(
            seasonId = seasonId,
            userId = userB,
            matchId = matchId,
            ratingBefore = beforeB,
            ratingAfter = newB,
            delta = deltaB,
            reason = "MATCH_RESULT",
            vsBot = false
        )
    }

    fun listWaitingRooms(page: Int, size: Int): PageResponse<BattleRoomSummaryResponse> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)

        val (rows, total) = matchRepo.listWaitingRoomsPage(safePage, safeSize)

        val totalPages = if (total == 0L) 0 else ((total + safeSize - 1) / safeSize).toInt()
        val hasNext = safePage + 1 < totalPages

        return PageResponse(
            content = rows.map {
                BattleRoomSummaryResponse(
                    matchId = it.matchId,
                    matchType = it.matchType,
                    mode = it.mode,
                    status = it.status,
                    ownerUserId = it.ownerUserId,
                    currentPlayers = it.activeCount,
                    maxPlayers = MAX_PLAYERS,
                    createdAtEpochMs = it.createdAt
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )
            },
            page = safePage,
            size = safeSize,
            totalElements = total,
            totalPages = totalPages,
            hasNext = hasNext
        )
    }

    @Transactional
    fun getRoomDetail(userId: Long, matchId: Long): BattleRoomDetailResponse {
        val info = matchRepo.getMatchInfo(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        val owner = matchRepo.findOwnerUserId(matchId)

        val parts = partRepo.listActiveParticipantsForRoom(
            seasonId = info.seasonId,
            matchId = matchId
        )

        return BattleRoomDetailResponse(
            matchId = matchId,
            matchType = info.matchType,
            mode = info.mode,
            status = info.status,
            ownerUserId = owner,
            maxPlayers = MAX_PLAYERS,
            participants = parts.map {
                BattleRoomParticipantResponse(
                    userId = it.userId,
                    team = it.team,
                    characterId = it.characterId,
                    characterName = it.characterName,
                    rating = it.rating,
                    wins = it.wins,
                    losses = it.losses,
                    draws = it.draws,
                    isOwner = (owner != null && owner == it.userId),
                    isReady = it.readyAt != null
                )
            }
        )
    }

    @Transactional
    fun joinRoom(matchId: Long, userId: Long, characterId: Long?): Long {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (status != BattleMatchStatus.WAITING) {
            throw ApiException(ErrorCode.BATTLE_MATCH_NOT_JOINABLE)
        }

        // ✅ 멱등(이미 active 참가자면 OK)
        if (partRepo.existsActiveParticipant(matchId, userId)) return matchId

        val count = partRepo.countActiveParticipants(matchId)
        if (count >= MAX_PLAYERS) throw ApiException(ErrorCode.BATTLE_MATCH_FULL)

        val cid = characterId ?: characterRepo.findDefaultCharacterId()
        ?: throw ApiException(ErrorCode.NOT_FOUND)
        val cver = characterRepo.findCharacterVersionNo(cid)

        val team = when (count) {
            0L -> BattleTeam.A
            1L -> BattleTeam.B
            else -> throw ApiException(ErrorCode.BATTLE_MATCH_FULL)
        }

        partRepo.insertParticipant(matchId, userId, team, cid, cver)

        return matchId
    }

    fun listCharacters(): List<BattleCharacterResponse> =
        characterRepo.listActiveCharacters().map {
            BattleCharacterResponse(
                id = it.id,
                code = it.code,
                name = it.name,
                description = it.description,
                versionNo = it.versionNo
            )
        }

    @Transactional
    fun changeCharacter(userId: Long, matchId: Long, characterId: Long) {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (status != BattleMatchStatus.WAITING) {
            // RUNNING에서 바꾸게 할지 정책 선택
            throw ApiException(ErrorCode.BATTLE_CHARACTER_CHANGE_NOT_ALLOWED)
        }

        // 참가자인지 확인(active)
        if (!partRepo.existsActiveParticipant(matchId, userId)) {
            throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
        }

        // 캐릭터 유효성(활성/삭제)
        val versionNo = characterRepo.findCharacterVersionNo(characterId)
        // findCharacterVersionNo가 "없으면 예외"를 던지지 않으면 여기서 NOT_FOUND 처리

        val updated = partRepo.updateCharacter(matchId, userId, characterId, versionNo)
        if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
    }

    @Transactional
    fun setRoomReady(userId: Long, matchId: Long, ready: Boolean) {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (status != BattleMatchStatus.WAITING) {
            throw ApiException(ErrorCode.BATTLE_READY_NOT_ALLOWED)
        }

        // 참가자 아니면 ready 불가
        if (!partRepo.existsActiveParticipant(matchId, userId)) {
            throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
        }

        val readyAt = if (ready) java.time.LocalDateTime.now() else null
        val updated = partRepo.setReady(matchId, userId, readyAt)
        if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        // ✅ 둘 다 들어와 있고, 둘 다 ready면 시작
        val active = partRepo.countActiveParticipants(matchId)
        val readyCount = partRepo.countReadyActiveParticipants(matchId)

        if (active == MAX_PLAYERS.toLong() && readyCount == MAX_PLAYERS.toLong()) {
            matchRepo.updateMatchToRunning(matchId)
            scheduleMatchFinish(matchId)
        }
    }

    @Transactional
    fun startRoom(userId: Long, matchId: Long) {
        val info = matchRepo.getMatchInfo(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (info.status != BattleMatchStatus.WAITING) {
            throw ApiException(ErrorCode.BATTLE_START_CONDITION_NOT_MET)
        }

        val owner = matchRepo.findOwnerUserId(matchId)
        if (owner == null || owner != userId) {
            throw ApiException(ErrorCode.BATTLE_START_NOT_ALLOWED)
        }

        val active = partRepo.countActiveParticipants(matchId)
        val ready = partRepo.countReadyActiveParticipants(matchId)

        if (active != MAX_PLAYERS.toLong() || ready != MAX_PLAYERS.toLong()) {
            throw ApiException(ErrorCode.BATTLE_START_CONDITION_NOT_MET)
        }

        // ✅ 원자적 시작 (WAITING일 때만 RUNNING)
        val updated = matchRepo.startIfWaiting(matchId)
        if (updated == 0) return // 멱등: 누가 먼저 시작했으면 조용히 종료(또는 예외)

        scheduleMatchFinish(matchId)
    }

    private fun expected(rA: Int, rB: Int): Double =
        1.0 / (1.0 + 10.0.pow((rB - rA) / 400.0))
}