package com.blog.domain.battle.repository

import com.blog.domain.battle.dto.response.MatchInfo
import com.blog.domain.battle.dto.response.WaitingRoomRow
import com.blog.domain.battle.dto.response.WaitingRoomsPage
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode
import com.blog.jooq.Tables.BATTLE_MATCHES
import com.blog.jooq.Tables.BATTLE_MATCH_PARTICIPANTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BattleMatchJooqRepository(
    private val dsl: DSLContext
) {
    fun insertMatch(
        seasonId: Long,
        matchType: BattleMatchType,
        mode: BattleMode,
        createdByUserId: Long?,
        focusLane: Int?
    ): Long {
        val now = LocalDateTime.now()
        return dsl.insertInto(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.SEASON_ID, seasonId)
            .set(BATTLE_MATCHES.MATCH_TYPE, matchType.name)
            .set(BATTLE_MATCHES.MODE, mode.name)
            .set(BATTLE_MATCHES.STATUS, "WAITING")
            .set(BATTLE_MATCHES.LANES, 3)
            .set(BATTLE_MATCHES.DURATION_MS, 30000)
            .set(BATTLE_MATCHES.P_MAX, 100)
            .set(BATTLE_MATCHES.FOCUS_LANE, focusLane)
            .set(BATTLE_MATCHES.CREATED_BY_USER_ID, createdByUserId)
            .set(BATTLE_MATCHES.HAS_BOT, false)
            .set(BATTLE_MATCHES.CREATED_AT, now)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .returning(BATTLE_MATCHES.ID)
            .fetchOne()!!
            .id!!
    }

    fun updateMatchToRunning(matchId: Long) {
        val now = LocalDateTime.now()
        dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.STATUS, BattleMatchStatus.RUNNING.name)
            .set(BATTLE_MATCHES.STARTED_AT, now)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .execute()
    }

    fun updateMatchToFinished(matchId: Long) {
        dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.STATUS, BattleMatchStatus.FINISHED.name)
            .set(BATTLE_MATCHES.ENDED_AT, LocalDateTime.now())
            .set(BATTLE_MATCHES.UPDATED_AT, LocalDateTime.now())
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .execute()
    }

    fun findMatchStatus(matchId: Long): BattleMatchStatus? =
        dsl.select(BATTLE_MATCHES.STATUS)
            .from(BATTLE_MATCHES)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .fetchOne(BATTLE_MATCHES.STATUS)
            ?.let(::toMatchStatus)

    fun getSeasonId(matchId: Long): Long? =
        dsl.select(BATTLE_MATCHES.SEASON_ID)
            .from(BATTLE_MATCHES)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .fetchOne(BATTLE_MATCHES.SEASON_ID)

    fun getMatchInfo(matchId: Long): MatchInfo? =
        dsl.select(
            BATTLE_MATCHES.SEASON_ID,
            BATTLE_MATCHES.MATCH_TYPE,
            BATTLE_MATCHES.MODE,
            BATTLE_MATCHES.STATUS
        )
            .from(BATTLE_MATCHES)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .fetchOne { r ->
                MatchInfo(
                    seasonId = r.get(BATTLE_MATCHES.SEASON_ID),
                    matchType = toMatchType(r.get(BATTLE_MATCHES.MATCH_TYPE)),
                    mode = toMode(r.get(BATTLE_MATCHES.MODE)),
                    status = toMatchStatus(r.get(BATTLE_MATCHES.STATUS)),
                )
            }

    /** RUNNING인 경우에만 FINISHED로 전환. 성공하면 1, 아니면 0 */
    fun finishIfRunning(matchId: Long): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.STATUS, BattleMatchStatus.FINISHED.name)
            .set(BATTLE_MATCHES.ENDED_AT, now)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .and(BATTLE_MATCHES.STATUS.eq(BattleMatchStatus.RUNNING.name))
            .execute()
    }

    fun lockJoinableMatchId(matchType: BattleMatchType, mode: BattleMode, maxPlayers: Int): Long? {
        val m = BATTLE_MATCHES.`as`("m")
        val p = BATTLE_MATCH_PARTICIPANTS.`as`("p")

        val activeCount = org.jooq.impl.DSL.count(p.ID)

        return dsl
            .select(m.ID)
            .from(m)
            .leftJoin(p).on(p.MATCH_ID.eq(m.ID).and(p.LEFT_AT.isNull)) // ✅ active only
            .where(m.STATUS.eq(BattleMatchStatus.WAITING.name))
            .and(m.MATCH_TYPE.eq(matchType.name))
            .and(m.MODE.eq(mode.name))
            .groupBy(m.ID)
            .having(activeCount.lt(maxPlayers)) // ✅ < maxPlayers
            .orderBy(m.CREATED_AT.asc())
            .limit(1)
            .forUpdate()
            .skipLocked()
            .fetchOne(m.ID)
    }

    fun findOwnerUserId(matchId: Long): Long? =
        dsl.select(BATTLE_MATCHES.CREATED_BY_USER_ID)
            .from(BATTLE_MATCHES)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .fetchOne(BATTLE_MATCHES.CREATED_BY_USER_ID)

    fun updateOwner(matchId: Long, newOwnerUserId: Long): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.CREATED_BY_USER_ID, newOwnerUserId)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .execute()
    }

    fun cancelIfWaiting(matchId: Long): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.STATUS, BattleMatchStatus.CANCELED.name)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .and(BATTLE_MATCHES.STATUS.eq(BattleMatchStatus.WAITING.name))
            .execute()
    }

    fun listWaitingRoomsPage(page: Int, size: Int): WaitingRoomsPage {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val offset = safePage * safeSize

        val m = BATTLE_MATCHES.`as`("m")
        val p = BATTLE_MATCH_PARTICIPANTS.`as`("p")
        val activeCount = org.jooq.impl.DSL.count(p.ID).`as`("activeCount")

        // ✅ 1) content 쿼리
        val rows = dsl.select(
            m.ID, m.MATCH_TYPE, m.MODE, m.STATUS, m.CREATED_BY_USER_ID, activeCount, m.CREATED_AT
        )
            .from(m)
            .leftJoin(p).on(p.MATCH_ID.eq(m.ID).and(p.LEFT_AT.isNull))
            .where(m.STATUS.eq(BattleMatchStatus.WAITING.name))
            .groupBy(m.ID, m.MATCH_TYPE, m.MODE, m.STATUS, m.CREATED_BY_USER_ID, m.CREATED_AT)
            .having(activeCount.gt(0)) // 고아방 제외(원하면 제거)
            .orderBy(m.CREATED_AT.desc())
            .limit(safeSize)
            .offset(offset)
            .fetch { r ->
                WaitingRoomRow(
                    matchId = r.get(m.ID)!!,
                    matchType = toMatchType(r.get(m.MATCH_TYPE)),
                    mode = toMode(r.get(m.MODE)),
                    status = toMatchStatus(r.get(m.STATUS)),
                    ownerUserId = r.get(m.CREATED_BY_USER_ID),
                    activeCount = r.get("activeCount", Int::class.java) ?: 0,
                    createdAt = r.get(m.CREATED_AT)!!
                )
            }

        // ✅ 2) total 쿼리 (WAITING 방 개수)
        // groupBy/having 때문에 "그룹 수"를 세야 함.
        val total = dsl.selectCount()
            .from(
                dsl.select(m.ID)
                    .from(m)
                    .leftJoin(p).on(p.MATCH_ID.eq(m.ID).and(p.LEFT_AT.isNull))
                    .where(m.STATUS.eq(BattleMatchStatus.WAITING.name))
                    .groupBy(m.ID)
                    .having(org.jooq.impl.DSL.count(p.ID).gt(0))
                    .asTable("t")
            )
            .fetchOne(0, Long::class.java) ?: 0L

        return WaitingRoomsPage(rows, total)
    }

    fun startIfWaiting(matchId: Long): Int {
        val now = java.time.LocalDateTime.now()
        return dsl.update(BATTLE_MATCHES)
            .set(BATTLE_MATCHES.STATUS, BattleMatchStatus.RUNNING.name)
            .set(BATTLE_MATCHES.STARTED_AT, now)
            .set(BATTLE_MATCHES.UPDATED_AT, now)
            .where(BATTLE_MATCHES.ID.eq(matchId))
            .and(BATTLE_MATCHES.STATUS.eq(BattleMatchStatus.WAITING.name))
            .execute()
    }

    // -------- enum safe parsers --------
    private fun toMatchStatus(s: String): BattleMatchStatus =
        runCatching { BattleMatchStatus.valueOf(s) }
            .getOrElse { throw IllegalStateException("Invalid match status in DB: $s") }

    private fun toMatchType(s: String): BattleMatchType =
        runCatching { BattleMatchType.valueOf(s) }
            .getOrElse { throw IllegalStateException("Invalid match type in DB: $s") }

    private fun toMode(s: String): BattleMode =
        runCatching { BattleMode.valueOf(s) }
            .getOrElse { throw IllegalStateException("Invalid mode in DB: $s") }
}