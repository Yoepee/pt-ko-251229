package com.blog.domain.battle.repository

import com.blog.jooq.Tables.BATTLE_MATCH_RESULTS
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BattleResultJooqRepository(
    private val dsl: DSLContext
) {
    fun exists(matchId: Long): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(BATTLE_MATCH_RESULTS)
                .where(BATTLE_MATCH_RESULTS.MATCH_ID.eq(matchId))
        )

    fun insertResult(
        matchId: Long,
        winnerTeam: String,
        endReason: String,
        lane0: Int,
        lane1: Int,
        lane2: Int,
        inputsA: Int,
        inputsB: Int,
        extraStatsJson: String = "{}"
    ) {
        dsl.insertInto(BATTLE_MATCH_RESULTS)
            .set(BATTLE_MATCH_RESULTS.MATCH_ID, matchId)
            .set(BATTLE_MATCH_RESULTS.WINNER_TEAM, winnerTeam)
            .set(BATTLE_MATCH_RESULTS.END_REASON, endReason)
            .set(BATTLE_MATCH_RESULTS.LANE0_FINAL, lane0)
            .set(BATTLE_MATCH_RESULTS.LANE1_FINAL, lane1)
            .set(BATTLE_MATCH_RESULTS.LANE2_FINAL, lane2)
            .set(BATTLE_MATCH_RESULTS.INPUTS_TEAM_A, inputsA)
            .set(BATTLE_MATCH_RESULTS.INPUTS_TEAM_B, inputsB)
            .set(BATTLE_MATCH_RESULTS.EXTRA_STATS, JSON.valueOf(extraStatsJson))
            .set(BATTLE_MATCH_RESULTS.CREATED_AT, LocalDateTime.now())
            .set(BATTLE_MATCH_RESULTS.UPDATED_AT, LocalDateTime.now())
            .execute()
    }
}