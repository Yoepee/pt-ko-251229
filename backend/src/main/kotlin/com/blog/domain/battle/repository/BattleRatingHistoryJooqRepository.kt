package com.blog.domain.battle.repository

import com.blog.jooq.Tables.BATTLE_RATING_HISTORY
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BattleRatingHistoryJooqRepository(
    private val dsl: DSLContext
) {
    fun insert(
        seasonId: Long,
        userId: Long,
        matchId: Long,
        ratingBefore: Int,
        ratingAfter: Int,
        delta: Int,
        reason: String = "MATCH_RESULT",
        vsBot: Boolean = false
    ) {
        val now = LocalDateTime.now()
        dsl.insertInto(BATTLE_RATING_HISTORY)
            .set(BATTLE_RATING_HISTORY.SEASON_ID, seasonId)
            .set(BATTLE_RATING_HISTORY.USER_ID, userId)
            .set(BATTLE_RATING_HISTORY.MATCH_ID, matchId)
            .set(BATTLE_RATING_HISTORY.RATING_BEFORE, ratingBefore)
            .set(BATTLE_RATING_HISTORY.RATING_AFTER, ratingAfter)
            .set(BATTLE_RATING_HISTORY.DELTA, delta)
            .set(BATTLE_RATING_HISTORY.REASON, reason)
            .set(BATTLE_RATING_HISTORY.VS_BOT, vsBot)
            .set(BATTLE_RATING_HISTORY.CREATED_AT, now)
            .set(BATTLE_RATING_HISTORY.UPDATED_AT, now)
            .execute()
    }
}