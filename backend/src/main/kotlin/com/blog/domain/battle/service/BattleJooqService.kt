package com.blog.domain.battle.service

import com.blog.domain.battle.dto.realtime.LobbyEvent
import com.blog.domain.battle.dto.realtime.LobbyEventType
import com.blog.domain.battle.dto.realtime.ReadyChangedPayload
import com.blog.domain.battle.dto.realtime.RoomEvent
import com.blog.domain.battle.dto.realtime.RoomEventType
import com.blog.domain.battle.dto.realtime.WsFinishedPayload
import com.blog.domain.battle.dto.request.AutoMatchRequest
import com.blog.domain.battle.dto.request.BattleInputRequest
import com.blog.domain.battle.dto.request.CreateRoomRequest
import com.blog.domain.battle.dto.request.RoomOrder
import com.blog.domain.battle.dto.response.AutoMatchResponse
import com.blog.domain.battle.dto.response.BattleCharacterResponse
import com.blog.domain.battle.dto.response.BattleMatchDetailResponse
import com.blog.domain.battle.dto.response.BattleRoomDetailResponse
import com.blog.domain.battle.dto.response.BattleRoomParticipantResponse
import com.blog.domain.battle.dto.response.BattleRoomSnapshotResponse
import com.blog.domain.battle.dto.response.BattleRoomSummaryResponse
import com.blog.domain.battle.dto.response.LaneSnapshot
import com.blog.domain.battle.dto.response.LobbyStateType
import com.blog.domain.battle.dto.response.MyBattleStatsResponse
import com.blog.domain.battle.dto.response.MyLobbyStateResponse
import com.blog.domain.battle.entity.BattleEndReason
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode
import com.blog.domain.battle.entity.BattleTeam
import com.blog.domain.battle.entity.BattleWinnerTeam
import com.blog.domain.battle.repository.BattleCharacterJooqRepository
import com.blog.domain.battle.repository.BattleMatchJooqRepository
import com.blog.domain.battle.repository.BattleParticipantJooqRepository
import com.blog.domain.battle.repository.BattleRatingHistoryJooqRepository
import com.blog.domain.battle.repository.BattleRatingJooqRepository
import com.blog.domain.battle.repository.BattleSeasonJooqRepository
import com.blog.domain.battle.repository.BattleResultJooqRepository
import com.blog.global.common.PageResponse
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import com.blog.global.realtime.RealtimeKeys
import com.blog.global.realtime.SseHub
import com.blog.global.realtime.WsSessionRegistry
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
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
    private val battleRedis: BattleRedisPort,
    private val sseHub: SseHub,
    private val objectMapper: ObjectMapper,
    private val registry: WsSessionRegistry
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val DURATION_MS = 30_000L
    private val MAX_PLAYERS = 2

    // -------------------------
    // Match Create / Join
    // -------------------------

    @Transactional
    fun createRankedMatch(userId: Long, mode: BattleMode): Long {
        val seasonId = seasonRepo.findActiveSeasonId()
            ?: throw ApiException(ErrorCode.NOT_FOUND)

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

        if (!partRepo.existsActiveParticipant(matchId, userId)) return

        val affected = partRepo.markLeft(matchId, userId)
        if (affected == 0) return

        val remain = partRepo.countActiveParticipants(matchId)
        if (remain == 0L) {
            matchRepo.cancelIfWaiting(matchId)
            emitRoomEvent(matchId, RoomEvent(RoomEventType.MATCH_CANCELED, matchId, null))
            emitRoomSnapshot(matchId)
            return
        }

        val owner = matchRepo.findOwnerUserId(matchId)
        if (owner == userId) {
            val newOwner = partRepo.findNextOwnerUserId(matchId)
            if (newOwner == null) {
                matchRepo.cancelIfWaiting(matchId)
                emitRoomEvent(matchId, RoomEvent(RoomEventType.MATCH_CANCELED, matchId, null))
                emitRoomSnapshot(matchId)
                return
            }
            matchRepo.updateOwner(matchId, newOwner)
            emitRoomEvent(matchId, RoomEvent(RoomEventType.OWNER_CHANGED, matchId, mapOf("ownerUserId" to newOwner)))
        }

        emitRoomSnapshot(matchId)
    }

    // -------------------------
    // Auto Match
    // -------------------------

    @Transactional
    fun autoMatch(userId: Long, req: AutoMatchRequest): AutoMatchResponse {
        val seasonId = seasonRepo.findActiveSeasonId()
            ?: throw ApiException(ErrorCode.BATTLE_SEASON_NOT_FOUND)

        val joinableMatchId = matchRepo.lockJoinableMatchId(req.matchType, req.mode, MAX_PLAYERS)
        val shouldAutoStart = (req.matchType == BattleMatchType.RANKED)

        return if (joinableMatchId != null) {
            val team = joinAs(joinableMatchId, userId, preferTeam = BattleTeam.B, characterId = req.characterId)
            val count = partRepo.countActiveParticipants(joinableMatchId)

            if (shouldAutoStart && count >= MAX_PLAYERS.toLong()) {
                matchRepo.updateMatchToRunning(joinableMatchId)
                scheduleMatchFinish(joinableMatchId)
                battleRedis.addRunningMatch(joinableMatchId)
                emitRoomEvent(joinableMatchId, RoomEvent(RoomEventType.MATCH_STARTED, joinableMatchId, null))
                emitRoomSnapshot(joinableMatchId)
                AutoMatchResponse(joinableMatchId, BattleMatchStatus.RUNNING, team)
            } else {
                emitRoomSnapshot(joinableMatchId)
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
            emitRoomSnapshot(matchId)
            AutoMatchResponse(matchId, BattleMatchStatus.WAITING, team)
        }
    }

    private fun joinAs(matchId: Long, userId: Long, preferTeam: BattleTeam, characterId: Long?): BattleTeam {
        partRepo.findActiveTeam(matchId, userId)?.let { return it }

        val cid = characterId ?: characterRepo.findDefaultCharacterId()
        ?: throw ApiException(ErrorCode.NOT_FOUND)
        val cver = characterRepo.findCharacterVersionNo(cid)

        fun pickTeam(): BattleTeam {
            val aTaken = partRepo.existsActiveTeam(matchId, BattleTeam.A)
            val bTaken = partRepo.existsActiveTeam(matchId, BattleTeam.B)

            if (preferTeam == BattleTeam.A && !aTaken) return BattleTeam.A
            if (preferTeam == BattleTeam.B && !bTaken) return BattleTeam.B

            return when {
                !aTaken -> BattleTeam.A
                !bTaken -> BattleTeam.B
                else -> throw ApiException(ErrorCode.BATTLE_MATCH_FULL)
            }
        }

        val team = pickTeam()
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
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.RUNNING) throw ApiException(ErrorCode.BATTLE_MATCH_NOT_RUNNING)

        val team = partRepo.findActiveTeam(matchId, userId)
            ?: throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        val ok = battleRedis.submitInput(matchId, userId, req.lane, req.power, team)
        if (!ok) throw ApiException(ErrorCode.BATTLE_RATE_LIMIT_EXCEEDED)

        val snap = battleRedis.getLaneSnapshot(matchId)
        val earlyWinnerTeam: BattleTeam? = detectEarlyWinner(snap) // 너 규칙에 맞게 구현
        if (earlyWinnerTeam != null) {
            finishEarlyWin(matchId, earlyWinnerTeam, extra = mapOf("by" to "EARLY_WIN_RULE"))
        }
    }

    @Transactional
    fun getMatchDetail(userId: Long, matchId: Long): BattleMatchDetailResponse {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        val myTeam = partRepo.findActiveTeam(matchId, userId)
        val snap = battleRedis.getLaneSnapshot(matchId)
        val endsAt = if (status == BattleMatchStatus.RUNNING) battleRedis.getEndsAt(matchId) else null

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
        if (info.status != BattleMatchStatus.RUNNING) return

        val snap = battleRedis.getLaneSnapshot(matchId)
        val sumA = snap.lane0A + snap.lane1A + snap.lane2A
        val sumB = snap.lane0B + snap.lane1B + snap.lane2B

        val winner: BattleWinnerTeam = when {
            sumA > sumB -> BattleWinnerTeam.A
            sumB > sumA -> BattleWinnerTeam.B
            else -> BattleWinnerTeam.DRAW
        }

        val lane0 = snap.lane0A + snap.lane0B
        val lane1 = snap.lane1A + snap.lane1B
        val lane2 = snap.lane2A + snap.lane2B

        val finishedNow = finishMatch(
            matchId = matchId,
            winner = winner,
            endReason = BattleEndReason.TIMEUP,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA, inputsB = snap.inputsB,
            vsBot = false,
            extraStatsJson = """{"sumA":$sumA,"sumB":$sumB}"""
        )

        if (!finishedNow) return

        afterFinished(
            matchId = matchId,
            winner = winner,
            reason = BattleEndReason.TIMEUP,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA, inputsB = snap.inputsB,
            extra = mapOf("sumA" to sumA, "sumB" to sumB)
        )
    }

    private fun applyEloAndHistory(
        seasonId: Long,
        matchId: Long,
        userA: Long,
        userB: Long,
        winnerTeam: BattleTeam?,
    ) {
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

        historyRepo.insert(seasonId, userA, matchId, beforeA, newA, deltaA, "MATCH_RESULT", false)
        historyRepo.insert(seasonId, userB, matchId, beforeB, newB, deltaB, "MATCH_RESULT", false)
    }

    fun listWaitingRooms(pageable: Pageable): PageResponse<BattleRoomSummaryResponse> {
        val safePage = pageable.pageNumber.coerceAtLeast(0)
        val safeSize = pageable.pageSize.coerceIn(1, 100)

        val sort = pageable.sort
        val order = when {
            sort.getOrderFor("createdAt")?.isDescending == true -> RoomOrder.CREATED_AT_DESC
            sort.getOrderFor("createdAt")?.isAscending == true -> RoomOrder.CREATED_AT_ASC
            else -> RoomOrder.CREATED_AT_DESC
        }

        val (rows, total) = matchRepo.listWaitingRoomsPage(safePage, safeSize, order)

        val totalPages = if (total == 0L) 0 else ((total + safeSize - 1) / safeSize).toInt()
        val hasNext = safePage + 1 < totalPages

        return PageResponse(
            content = rows.map { r ->
                BattleRoomSummaryResponse(
                    matchId = r.matchId,
                    matchType = r.matchType,
                    mode = r.mode,
                    status = r.status,
                    ownerUserId = r.ownerUserId,
                    currentPlayers = r.activeCount,
                    maxPlayers = MAX_PLAYERS,
                    createdAtEpochMs = r.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
        val parts = partRepo.listActiveParticipantsForRoom(info.seasonId, matchId)
        val active = partRepo.countActiveParticipants(matchId)
        val ready = partRepo.countReadyActiveParticipants(matchId)

        val canStart = (info.status == BattleMatchStatus.WAITING) &&
                (owner != null && owner == userId) &&
                (active == MAX_PLAYERS.toLong()) &&
                (ready == MAX_PLAYERS.toLong())

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
            },
            canStart = canStart,
        )
    }

    @Transactional
    fun joinRoom(matchId: Long, userId: Long, characterId: Long?): Long {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.WAITING) throw ApiException(ErrorCode.BATTLE_MATCH_NOT_JOINABLE)

        if (partRepo.existsActiveParticipant(matchId, userId)) return matchId

        val cid = characterId ?: characterRepo.findDefaultCharacterId()
        ?: throw ApiException(ErrorCode.NOT_FOUND)
        val cver = characterRepo.findCharacterVersionNo(cid)

        fun pickTeam(): BattleTeam {
            val aTaken = partRepo.existsActiveTeam(matchId, BattleTeam.A)
            val bTaken = partRepo.existsActiveTeam(matchId, BattleTeam.B)
            return when {
                !aTaken -> BattleTeam.A
                !bTaken -> BattleTeam.B
                else -> throw ApiException(ErrorCode.BATTLE_MATCH_FULL)
            }
        }

        val team = pickTeam()
        val revived = partRepo.rejoin(matchId, userId, team, cid, cver)
        if (revived == 0) {
            val finalTeam = pickTeam()
            partRepo.insertParticipant(matchId, userId, finalTeam, cid, cver)
        }

        emitRoomSnapshot(matchId)
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
            throw ApiException(ErrorCode.BATTLE_CHARACTER_CHANGE_NOT_ALLOWED)
        }

        if (!partRepo.existsActiveParticipant(matchId, userId)) {
            throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
        }

        val versionNo = characterRepo.findCharacterVersionNo(characterId)
        val updated = partRepo.updateCharacter(matchId, userId, characterId, versionNo)
        if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        emitRoomSnapshot(matchId)
    }

    @Transactional
    fun setRoomReady(userId: Long, matchId: Long, ready: Boolean) {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (status != BattleMatchStatus.WAITING) {
            throw ApiException(ErrorCode.BATTLE_READY_NOT_ALLOWED)
        }

        if (!partRepo.existsActiveParticipant(matchId, userId)) {
            throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
        }

        val readyAt = if (ready) java.time.LocalDateTime.now() else null
        val updated = partRepo.setReady(matchId, userId, readyAt)
        if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        emitRoomEvent(matchId, RoomEvent(RoomEventType.READY_CHANGED, matchId, ReadyChangedPayload(userId, ready)))
        emitRoomSnapshot(matchId)
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

        val updated = matchRepo.startIfWaiting(matchId)
        if (updated == 0) return

        scheduleMatchFinish(matchId)
        battleRedis.addRunningMatch(matchId)

        emitRoomEvent(matchId, RoomEvent(RoomEventType.MATCH_STARTED, matchId, null))
        emitRoomSnapshot(matchId)
    }

    @Transactional
    fun kickFromRoom(ownerUserId: Long, matchId: Long, targetUserId: Long) {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.WAITING) throw ApiException(ErrorCode.BATTLE_KICK_NOT_ALLOWED)

        val owner = matchRepo.findOwnerUserId(matchId)
        if (owner == null || owner != ownerUserId) throw ApiException(ErrorCode.BATTLE_KICK_NOT_ALLOWED)

        if (targetUserId == ownerUserId) throw ApiException(ErrorCode.BATTLE_KICK_NOT_ALLOWED)

        if (!partRepo.existsActiveParticipant(matchId, targetUserId)) return

        val affected = partRepo.markLeft(matchId, targetUserId)
        if (affected == 0) return

        val remain = partRepo.countActiveParticipants(matchId)
        if (remain == 0L) {
            matchRepo.cancelIfWaiting(matchId)
            emitRoomEvent(matchId, RoomEvent(RoomEventType.MATCH_CANCELED, matchId, null))
            emitRoomSnapshot(matchId)
            return
        }

        emitRoomEvent(matchId, RoomEvent(RoomEventType.USER_KICKED, matchId, mapOf("userId" to targetUserId)))
        emitRoomSnapshot(matchId)
    }

    @Transactional
    fun changeTeam(userId: Long, matchId: Long, toTeam: BattleTeam) {
        val status = matchRepo.findMatchStatus(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)
        if (status != BattleMatchStatus.WAITING) throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_NOT_ALLOWED)

        val myTeam = partRepo.findActiveTeam(matchId, userId)
            ?: throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        if (myTeam == toTeam) return

        if (partRepo.isReady(matchId, userId)) {
            throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_REQUIRES_NOT_READY)
        }

        val active = partRepo.countActiveParticipants(matchId)

        if (MAX_PLAYERS == 2) {
            if (active == 1L) {
                val updated = partRepo.updateTeam(matchId, userId, toTeam)
                if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
                emitRoomEvent(matchId,
                    RoomEvent(RoomEventType.TEAM_CHANGED, matchId, mapOf("userId" to userId, "team" to toTeam.name))
                )
                emitRoomSnapshot(matchId)
                return
            }

            val otherUserId = partRepo.findOtherActiveUserId(matchId, userId)
                ?: throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_NOT_ALLOWED)

            if (partRepo.isReady(matchId, otherUserId)) {
                throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_REQUIRES_NOT_READY)
            }

            val otherTeam = partRepo.findActiveTeam(matchId, otherUserId)
                ?: throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_NOT_ALLOWED)

            if (toTeam != otherTeam) {
                throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_NOT_ALLOWED)
            }

            val swapped = partRepo.swapTeams(matchId, userId, otherUserId)
            if (swapped != 2) {
                throw ApiException(ErrorCode.BATTLE_TEAM_CHANGE_REQUIRES_NOT_READY)
            }

            emitRoomEvent(matchId,
                RoomEvent(RoomEventType.TEAM_SWAPPED, matchId, mapOf("a" to userId, "b" to otherUserId))
            )
            emitRoomSnapshot(matchId)
            return
        }

        val updated = partRepo.updateTeam(matchId, userId, toTeam)
        if (updated == 0) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)

        emitRoomEvent(matchId,
            RoomEvent(RoomEventType.TEAM_CHANGED, matchId, mapOf("userId" to userId, "team" to toTeam.name))
        )
        emitRoomSnapshot(matchId)
    }

    fun assertRoomReadable(userId: Long, matchId: Long) {
        val isParticipant = partRepo.existsActiveParticipant(matchId, userId)
        if (!isParticipant) throw ApiException(ErrorCode.BATTLE_NOT_PARTICIPANT)
    }

    private fun expected(rA: Int, rB: Int): Double =
        1.0 / (1.0 + 10.0.pow((rB - rA) / 400.0))

    private fun emitRoomEvent(matchId: Long, event: RoomEvent) {
        sseHub.publish(RealtimeKeys.room(matchId), event)
    }

    private fun emitRoomSnapshot(matchId: Long) {
        val snapshot = getRoomDetailForBroadcast(matchId)
        emitRoomEvent(matchId, RoomEvent(RoomEventType.ROOM_SNAPSHOT, matchId, snapshot))
    }

    @Transactional
    fun getRoomDetailForBroadcast(matchId: Long): BattleRoomSnapshotResponse {
        val info = matchRepo.getMatchInfo(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        val owner = matchRepo.findOwnerUserId(matchId)

        val parts = partRepo.listActiveParticipantsForRoom(info.seasonId, matchId)

        val active = partRepo.countActiveParticipants(matchId)
        val ready = partRepo.countReadyActiveParticipants(matchId)

        val canStart = (info.status == BattleMatchStatus.WAITING) &&
                (owner != null) &&
                (active == MAX_PLAYERS.toLong()) &&
                (ready == MAX_PLAYERS.toLong())

        return BattleRoomSnapshotResponse(
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
            },
            canStart = canStart
        )
    }

    fun getMyStats(userId: Long): MyBattleStatsResponse {
        val seasonId = seasonRepo.findActiveSeasonId()
            ?: throw ApiException(ErrorCode.BATTLE_SEASON_NOT_FOUND)

        // 없으면 insertIfAbsent로 기본 1500 세팅 (이미 너 로직에도 있음)
        ratingRepo.insertIfAbsent(seasonId, userId)

        val r = ratingRepo.getRatingRow(seasonId, userId)
            ?: throw ApiException(ErrorCode.BATTLE_RATING_NOT_FOUND)

        return MyBattleStatsResponse(
            seasonId = seasonId,
            userId = userId,
            rating = r.rating,
            matches = r.matches,
            wins = r.wins,
            losses = r.losses,
            draws = r.draws
        )
    }

    fun getMyLobbyState(userId: Long): MyLobbyStateResponse {
        val row = partRepo.findMyActiveMatch(userId) ?: return MyLobbyStateResponse(
            state = LobbyStateType.IDLE
        )

        val status = row.status
        val state = when (status) {
            BattleMatchStatus.WAITING -> LobbyStateType.WAITING
            BattleMatchStatus.RUNNING -> LobbyStateType.RUNNING
            else -> LobbyStateType.IDLE // FINISHED/CANCELED인데 left_at이 안 찍힌 경우(데이터 정리 필요)
        }

        return MyLobbyStateResponse(
            state = state,
            matchId = row.matchId,
            matchType = row.matchType,
            mode = row.mode,
            team = row.team,
            isOwner = (row.ownerUserId != null && row.ownerUserId == userId)
        )
    }

    @Transactional
    fun onWsConnected(userId: Long, matchId: Long) {
        battleRedis.clearDisconnected(matchId, userId) // 재접속이면 취소
    }

    @Transactional
    fun onWsDisconnected(userId: Long, matchId: Long) {
        val status = matchRepo.findMatchStatus(matchId) ?: return
        if (status != BattleMatchStatus.RUNNING) return

        // 참가자 아니면 무시
        partRepo.findActiveTeam(matchId, userId) ?: return

        // 3초 유예 마킹만
        val until = System.currentTimeMillis() + 3_000
        battleRedis.markDisconnected(matchId, userId, until)
    }

    @Transactional
    fun checkAndFinishForfeitIfNeeded(matchId: Long) {
        val status = matchRepo.findMatchStatus(matchId) ?: return
        if (status != BattleMatchStatus.RUNNING) return

        val users = partRepo.getTwoActivePlayersOrNull(matchId) ?: return
        val now = System.currentTimeMillis()

        val untilA = battleRedis.getDisconnectedUntil(matchId, users.userA)
        val untilB = battleRedis.getDisconnectedUntil(matchId, users.userB)

        val forfeitUserId =
            if (untilA != null && now >= untilA) users.userA
            else if (untilB != null && now >= untilB) users.userB
            else null
        if (forfeitUserId == null) return

        val myTeam = partRepo.findActiveTeam(matchId, forfeitUserId) ?: return
        val winnerTeam: BattleTeam = if (myTeam == BattleTeam.A) BattleTeam.B else BattleTeam.A
        val winner: BattleWinnerTeam = winnerTeam.toWinner()

        val snap = battleRedis.getLaneSnapshot(matchId)
        val lane0 = snap.lane0A + snap.lane0B
        val lane1 = snap.lane1A + snap.lane1B
        val lane2 = snap.lane2A + snap.lane2B

        val finishedNow = finishMatch(
            matchId = matchId,
            winner = winner,
            endReason = BattleEndReason.FORFEIT,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA,
            inputsB = snap.inputsB,
            vsBot = false,
            extraStatsJson = """{"forfeitUserId":$forfeitUserId}"""
        )
        if (!finishedNow) return

        battleRedis.clearDisconnected(matchId, forfeitUserId)

        afterFinished(
            matchId = matchId,
            winner = winner,
            reason = BattleEndReason.FORFEIT,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA, inputsB = snap.inputsB,
            extra = mapOf("forfeitUserId" to forfeitUserId)
        )
    }

    @Transactional
    fun finishMatch(
        matchId: Long,
        winner: BattleWinnerTeam,
        endReason: BattleEndReason,
        lane0: Int, lane1: Int, lane2: Int,
        inputsA: Int, inputsB: Int,
        vsBot: Boolean,
        extraStatsJson: String = "{}"
    ): Boolean {
        val info = matchRepo.getMatchInfo(matchId)
            ?: throw ApiException(ErrorCode.BATTLE_MATCH_NOT_FOUND)

        if (info.status != BattleMatchStatus.RUNNING) return false

        // ✅ 원자적 FINISHED 전환 (멱등)
        val updated = matchRepo.finishIfRunning(matchId)
        if (updated == 0) return false

        // ✅ 결과 저장
        resultRepo.insertResult(
            matchId = matchId,
            winnerTeam = winner,
            endReason = endReason,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = inputsA, inputsB = inputsB,
            extraStatsJson = extraStatsJson
        )

        // ✅ 레이팅 반영: ranked + 사람vs사람만
        if (info.matchType != BattleMatchType.RANKED || vsBot) return true

        val p = partRepo.getTwoActivePlayersOrNull(matchId) ?: return true

        // applyElo는 BattleTeam? (DRAW = null) 쓰고 있으니 변환
        applyEloAndHistory(
            seasonId = info.seasonId,
            matchId = matchId,
            userA = p.userA,
            userB = p.userB,
            winnerTeam = winner.toTeamOrNull()
        )

        return true
    }

    private fun broadcastFinished(
        matchId: Long,
        winner: BattleWinnerTeam,
        reason: BattleEndReason,
        lane0: Int, lane1: Int, lane2: Int,
        inputsA: Int, inputsB: Int,
        extra: Map<String, Any?> = emptyMap()
    ) {
        if (registry.getConnectedUserCount(matchId) == 0) return

        val payload = WsFinishedPayload(
            matchId = matchId,
            winner = winner,
            reason = reason,
            lane0 = lane0,
            lane1 = lane1,
            lane2 = lane2,
            inputsA = inputsA,
            inputsB = inputsB,
            extra = extra
        )

        val json = objectMapper.writeValueAsString(payload)
        registry.broadcastJson(matchId, json)
    }

    @Transactional
    fun finishEarlyWin(matchId: Long, winnerTeam: BattleTeam, extra: Map<String, Any?> = emptyMap()) {
        val snap = battleRedis.getLaneSnapshot(matchId)

        val winner = winnerTeam.toWinner()
        val lane0 = snap.lane0A + snap.lane0B
        val lane1 = snap.lane1A + snap.lane1B
        val lane2 = snap.lane2A + snap.lane2B

        val finishedNow = finishMatch(
            matchId = matchId,
            winner = winner,
            endReason = BattleEndReason.EARLY_WIN,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA, inputsB = snap.inputsB,
            vsBot = false,
            extraStatsJson = objectMapper.writeValueAsString(extra)
        )
        if (!finishedNow) return

        afterFinished(
            matchId = matchId,
            winner = winner,
            reason = BattleEndReason.EARLY_WIN,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = snap.inputsA, inputsB = snap.inputsB,
            extra = extra
        )
    }

    private fun afterFinished(
        matchId: Long,
        winner: BattleWinnerTeam,
        reason: BattleEndReason,
        lane0: Int, lane1: Int, lane2: Int,
        inputsA: Int, inputsB: Int,
        extra: Map<String, Any?> = emptyMap(),
    ) {
        // Redis 정리
        battleRedis.clearMatchKeys(matchId)
        battleRedis.removeRunningMatch(matchId)

        // ✅ WS FINISHED (즉시)
        broadcastFinished(
            matchId = matchId,
            winner = winner,
            reason = reason,
            lane0 = lane0, lane1 = lane1, lane2 = lane2,
            inputsA = inputsA, inputsB = inputsB,
            extra = extra
        )

        // SSE ROOM snapshot (WS 놓쳐도 동기화)
        emitRoomSnapshot(matchId)

        // SSE lobby 갱신
        emitLobbyRoomsChanged()
    }

    private fun detectEarlyWinner(s: LaneSnapshot, diffThreshold: Int = 50): BattleTeam? {
        val sumA = s.lane0A + s.lane1A + s.lane2A
        val sumB = s.lane0B + s.lane1B + s.lane2B

        val diff = kotlin.math.abs(sumA - sumB)
        if (diff < diffThreshold) return null

        return when {
            sumA > sumB -> BattleTeam.A
            sumB > sumA -> BattleTeam.B
            else -> null
        }
    }

    private fun onRoomChangedForLobbyAndRoom(matchId: Long) {
        emitRoomSnapshot(matchId)
        emitLobbyRoomsChanged()
    }

    private fun emitLobbyRoomsChanged() {
        sseHub.publish(
            RealtimeKeys.lobby(),
            LobbyEvent(type = LobbyEventType.ROOMS_CHANGED, payload = null)
        )
    }

    private fun BattleTeam.opposite() = if (this == BattleTeam.A) BattleTeam.B else BattleTeam.A
    fun BattleTeam.toWinner(): BattleWinnerTeam =
        if (this == BattleTeam.A) BattleWinnerTeam.A else BattleWinnerTeam.B

    fun BattleWinnerTeam.toTeamOrNull(): BattleTeam? =
        when (this) {
            BattleWinnerTeam.A -> BattleTeam.A
            BattleWinnerTeam.B -> BattleTeam.B
            BattleWinnerTeam.DRAW -> null
        }
}