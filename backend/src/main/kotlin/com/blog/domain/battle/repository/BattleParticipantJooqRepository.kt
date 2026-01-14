package com.blog.domain.battle.repository

import com.blog.domain.battle.dto.response.MyActiveMatchRow
import com.blog.domain.battle.dto.response.RoomParticipantRow
import com.blog.domain.battle.dto.response.TwoPlayers
import com.blog.domain.battle.entity.BattleMode
import com.blog.domain.battle.entity.BattleTeam
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.jooq.Tables.*
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BattleParticipantJooqRepository(
    private val dsl: DSLContext
) {

    fun countActiveParticipants(matchId: Long): Long =
        dsl.selectCount()
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .fetchOne(0, Long::class.java)!!

    fun existsActiveParticipant(matchId: Long, userId: Long): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(BATTLE_MATCH_PARTICIPANTS)
                .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
                .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
                .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
        )

    fun findActiveTeam(matchId: Long, userId: Long): BattleTeam? =
        dsl.select(BATTLE_MATCH_PARTICIPANTS.TEAM)
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .fetchOne(BATTLE_MATCH_PARTICIPANTS.TEAM)
            ?.let(::toBattleTeam)

    fun findActiveUsersByTeam(matchId: Long, team: BattleTeam): List<Long> =
        dsl.select(BATTLE_MATCH_PARTICIPANTS.USER_ID)
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.TEAM.eq(team.name))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .fetch(BATTLE_MATCH_PARTICIPANTS.USER_ID, Long::class.java)

    fun findMyActiveMatch(userId: Long): MyActiveMatchRow? {
        val p = BATTLE_MATCH_PARTICIPANTS.`as`("p")
        val m = BATTLE_MATCHES.`as`("m")

        return dsl.select(
            m.ID, m.STATUS, m.MATCH_TYPE, m.MODE, m.CREATED_BY_USER_ID,
            p.TEAM
        )
            .from(p)
            .join(m).on(p.MATCH_ID.eq(m.ID))
            .where(p.USER_ID.eq(userId).and(p.LEFT_AT.isNull))
            .orderBy(m.CREATED_AT.desc())
            .limit(1)
            .fetchOne { r ->
                MyActiveMatchRow(
                    matchId = r.get(m.ID)!!,
                    status = toMatchStatus(r.get(m.STATUS)),
                    matchType = toMatchType(r.get(m.MATCH_TYPE)),
                    mode = toMode(r.get(m.MODE)),
                    team = BattleTeam.valueOf(r.get(p.TEAM)!!.toString()),
                    ownerUserId = r.get(m.CREATED_BY_USER_ID)
                )
            }
    }

    fun getTwoActivePlayersOrNull(matchId: Long): TwoPlayers? {
        val rows = dsl.select(BATTLE_MATCH_PARTICIPANTS.TEAM, BATTLE_MATCH_PARTICIPANTS.USER_ID)
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .fetch()

        val a = rows.firstOrNull { toBattleTeam(it.get(BATTLE_MATCH_PARTICIPANTS.TEAM)) == BattleTeam.A }
            ?.get(BATTLE_MATCH_PARTICIPANTS.USER_ID)

        val b = rows.firstOrNull { toBattleTeam(it.get(BATTLE_MATCH_PARTICIPANTS.TEAM)) == BattleTeam.B }
            ?.get(BATTLE_MATCH_PARTICIPANTS.USER_ID)

        return if (a != null && b != null) TwoPlayers(a, b) else null
    }

    fun insertParticipant(matchId: Long, userId: Long, team: BattleTeam, characterId: Long, characterVersionNo: Int) {
        // joined_at이 NULL 허용인데, 정렬/방장 위임 때문에 NOW로 채우는 걸 추천
        dsl.insertInto(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.MATCH_ID, matchId)
            .set(BATTLE_MATCH_PARTICIPANTS.USER_ID, userId)
            .set(BATTLE_MATCH_PARTICIPANTS.TEAM, team.name) // CHAR(1)이라면 'A'/'B'
            .set(BATTLE_MATCH_PARTICIPANTS.IS_BOT, false)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_ID, characterId)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_VERSION_NO, characterVersionNo)
            .set(BATTLE_MATCH_PARTICIPANTS.JOINED_AT, LocalDateTime.now())
            .execute()
    }

    fun rejoin(matchId: Long, userId: Long, team: BattleTeam, characterId: Long, characterVersionNo: Int): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.TEAM, team.name)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_ID, characterId)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_VERSION_NO, characterVersionNo)
            .set(BATTLE_MATCH_PARTICIPANTS.JOINED_AT, now)
            .set(BATTLE_MATCH_PARTICIPANTS.LEFT_AT, null as LocalDateTime?)
            .set(BATTLE_MATCH_PARTICIPANTS.READY_AT, null as LocalDateTime?)
            .set(BATTLE_MATCH_PARTICIPANTS.UPDATED_AT, now)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNotNull)
            .execute()
    }

    fun markLeft(matchId: Long, userId: Long): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.LEFT_AT, now)
            .set(BATTLE_MATCH_PARTICIPANTS.UPDATED_AT, now)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .execute()
    }

    fun findNextOwnerUserId(matchId: Long): Long? =
        dsl.select(BATTLE_MATCH_PARTICIPANTS.USER_ID)
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .orderBy(
                BATTLE_MATCH_PARTICIPANTS.JOINED_AT.asc().nullsLast(),
                BATTLE_MATCH_PARTICIPANTS.CREATED_AT.asc()
            )
            .limit(1)
            .fetchOne(BATTLE_MATCH_PARTICIPANTS.USER_ID)

    fun listActiveParticipantsForRoom(seasonId: Long, matchId: Long): List<RoomParticipantRow> {
        val p = BATTLE_MATCH_PARTICIPANTS
        val c = BATTLE_CHARACTERS
        val r = BATTLE_USER_RATINGS

        return dsl.select(
            p.USER_ID,
            p.TEAM,
            p.CHARACTER_ID,
            c.NAME,
            r.RATING,
            r.WINS,
            r.LOSSES,
            r.DRAWS,
            p.READY_AT,
        )
            .from(p)
            .join(c).on(c.ID.eq(p.CHARACTER_ID))
            .leftJoin(r).on(r.SEASON_ID.eq(seasonId).and(r.USER_ID.eq(p.USER_ID)))
            .where(p.MATCH_ID.eq(matchId))
            .and(p.LEFT_AT.isNull)
            .orderBy(p.JOINED_AT.asc().nullsLast(), p.CREATED_AT.asc())
            .fetch { rec ->
                RoomParticipantRow(
                    userId = rec.get(p.USER_ID)!!,
                    team = BattleTeam.valueOf(rec.get(p.TEAM)!!.trim()),
                    characterId = rec.get(p.CHARACTER_ID)!!,
                    characterName = rec.get(c.NAME),
                    rating = rec.get(r.RATING),
                    wins = rec.get(r.WINS),
                    losses = rec.get(r.LOSSES),
                    draws = rec.get(r.DRAWS),
                    readyAt = rec.get(p.READY_AT),
                )
            }
    }

    fun updateCharacter(matchId: Long, userId: Long, characterId: Long, characterVersionNo: Int): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_ID, characterId)
            .set(BATTLE_MATCH_PARTICIPANTS.CHARACTER_VERSION_NO, characterVersionNo)
            .set(BATTLE_MATCH_PARTICIPANTS.UPDATED_AT, now)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .execute()
    }

    fun setReady(matchId: Long, userId: Long, readyAt: LocalDateTime?): Int {
        val now = LocalDateTime.now()
        return dsl.update(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.READY_AT, readyAt)
            .set(BATTLE_MATCH_PARTICIPANTS.UPDATED_AT, now)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .execute()
    }

    fun countReadyActiveParticipants(matchId: Long): Long =
        dsl.selectCount()
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .and(BATTLE_MATCH_PARTICIPANTS.READY_AT.isNotNull)
            .fetchOne(0, Long::class.java)!!

    fun updateTeam(matchId: Long, userId: Long, team: BattleTeam): Int {
        val now = java.time.LocalDateTime.now()
        return dsl.update(BATTLE_MATCH_PARTICIPANTS)
            .set(BATTLE_MATCH_PARTICIPANTS.TEAM, team.name)
            .set(BATTLE_MATCH_PARTICIPANTS.UPDATED_AT, now)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .execute()
    }

    fun findOtherActiveUserId(matchId: Long, userId: Long): Long? =
        dsl.select(BATTLE_MATCH_PARTICIPANTS.USER_ID)
            .from(BATTLE_MATCH_PARTICIPANTS)
            .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
            .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
            .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.ne(userId))
            .fetchOne(BATTLE_MATCH_PARTICIPANTS.USER_ID, Long::class.java)

    fun swapTeams(matchId: Long, userA: Long, userB: Long): Int {
        val p = BATTLE_MATCH_PARTICIPANTS
        val now = LocalDateTime.now()

        return dsl.update(p)
            .set(
                p.TEAM,
                DSL.`when`(p.USER_ID.eq(userA), BattleTeam.B.name)
                    .`when`(p.USER_ID.eq(userB), BattleTeam.A.name)
                    .otherwise(p.TEAM)
            )
            .set(p.UPDATED_AT, now)
            .where(p.MATCH_ID.eq(matchId))
            .and(p.LEFT_AT.isNull)
            .and(p.READY_AT.isNull)
            .and(p.USER_ID.`in`(userA, userB))
            .execute()
    }

    fun isReady(matchId: Long, userId: Long): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(BATTLE_MATCH_PARTICIPANTS)
                .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
                .and(BATTLE_MATCH_PARTICIPANTS.USER_ID.eq(userId))
                .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
                .and(BATTLE_MATCH_PARTICIPANTS.READY_AT.isNotNull)
        )

    fun existsActiveTeam(matchId: Long, team: BattleTeam): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(BATTLE_MATCH_PARTICIPANTS)
                .where(BATTLE_MATCH_PARTICIPANTS.MATCH_ID.eq(matchId))
                .and(BATTLE_MATCH_PARTICIPANTS.LEFT_AT.isNull)
                .and(BATTLE_MATCH_PARTICIPANTS.TEAM.eq(team.name))
        )

    // -------- enum safe parsers --------
    private fun toBattleTeam(s: String): BattleTeam =
        runCatching { BattleTeam.valueOf(s.trim()) }
            .getOrElse { throw IllegalStateException("Invalid match team in DB: $s") }

    private fun toMatchStatus(s: String): BattleMatchStatus =
        runCatching { BattleMatchStatus.valueOf(s.trim()) }
            .getOrElse { throw IllegalStateException("Invalid match status in DB: $s") }

    private fun toMatchType(s: String): BattleMatchType =
        runCatching { BattleMatchType.valueOf(s.trim()) }
            .getOrElse { throw IllegalStateException("Invalid match type in DB: $s") }

    private fun toMode(s: String): BattleMode =
        runCatching { BattleMode.valueOf(s.trim()) }
            .getOrElse { throw IllegalStateException("Invalid match mode in DB: $s") }
}