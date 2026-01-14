package com.blog.domain.battle.repository

import com.blog.domain.battle.dto.response.RatingRow
import com.blog.domain.battle.dto.response.UserRatingRow
import com.blog.jooq.Tables.BATTLE_USER_RATINGS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BattleRatingJooqRepository(
    private val dsl: DSLContext
) {
    fun lockRating(seasonId: Long, userId: Long): RatingRow? {
        val r = dsl.select(
            BATTLE_USER_RATINGS.RATING,
            BATTLE_USER_RATINGS.MATCHES,
            BATTLE_USER_RATINGS.WINS,
            BATTLE_USER_RATINGS.LOSSES,
            BATTLE_USER_RATINGS.DRAWS
        )
            .from(BATTLE_USER_RATINGS)
            .where(BATTLE_USER_RATINGS.SEASON_ID.eq(seasonId))
            .and(BATTLE_USER_RATINGS.USER_ID.eq(userId))
            .forUpdate()
            .fetchOne() ?: return null

        return RatingRow(
            rating = r.get(BATTLE_USER_RATINGS.RATING),
            matches = r.get(BATTLE_USER_RATINGS.MATCHES),
            wins = r.get(BATTLE_USER_RATINGS.WINS),
            losses = r.get(BATTLE_USER_RATINGS.LOSSES),
            draws = r.get(BATTLE_USER_RATINGS.DRAWS),
        )
    }

    fun insertIfAbsent(seasonId: Long, userId: Long, rating: Int = 1500) {
        dsl.insertInto(BATTLE_USER_RATINGS)
            .set(BATTLE_USER_RATINGS.SEASON_ID, seasonId)
            .set(BATTLE_USER_RATINGS.USER_ID, userId)
            .set(BATTLE_USER_RATINGS.RATING, rating)
            .set(BATTLE_USER_RATINGS.MATCHES, 0)
            .set(BATTLE_USER_RATINGS.WINS, 0)
            .set(BATTLE_USER_RATINGS.LOSSES, 0)
            .set(BATTLE_USER_RATINGS.DRAWS, 0)
            .set(BATTLE_USER_RATINGS.CREATED_AT, LocalDateTime.now())
            .set(BATTLE_USER_RATINGS.UPDATED_AT, LocalDateTime.now())
            .onConflict(BATTLE_USER_RATINGS.SEASON_ID, BATTLE_USER_RATINGS.USER_ID)
            .doNothing()
            .execute()
    }

    fun updateRating(
        seasonId: Long,
        userId: Long,
        newRating: Int,
        win: Int,
        loss: Int,
        draw: Int
    ) {
        dsl.update(BATTLE_USER_RATINGS)
            .set(BATTLE_USER_RATINGS.RATING, newRating)
            .set(BATTLE_USER_RATINGS.MATCHES, BATTLE_USER_RATINGS.MATCHES.plus(1))
            .set(BATTLE_USER_RATINGS.WINS, BATTLE_USER_RATINGS.WINS.plus(win))
            .set(BATTLE_USER_RATINGS.LOSSES, BATTLE_USER_RATINGS.LOSSES.plus(loss))
            .set(BATTLE_USER_RATINGS.DRAWS, BATTLE_USER_RATINGS.DRAWS.plus(draw))
            .set(BATTLE_USER_RATINGS.UPDATED_AT, LocalDateTime.now())
            .where(BATTLE_USER_RATINGS.SEASON_ID.eq(seasonId))
            .and(BATTLE_USER_RATINGS.USER_ID.eq(userId))
            .execute()
    }

    fun getRatingRow(seasonId: Long, userId: Long): UserRatingRow? {
        val r = BATTLE_USER_RATINGS.`as`("r")
        return dsl.select(r.RATING, r.MATCHES, r.WINS, r.LOSSES, r.DRAWS)
            .from(r)
            .where(r.SEASON_ID.eq(seasonId).and(r.USER_ID.eq(userId)))
            .fetchOne { rec ->
                UserRatingRow(
                    rating = rec.get(r.RATING)!!,
                    matches = rec.get(r.MATCHES)!!,
                    wins = rec.get(r.WINS)!!,
                    losses = rec.get(r.LOSSES)!!,
                    draws = rec.get(r.DRAWS)!!,
                )
            }
    }
}