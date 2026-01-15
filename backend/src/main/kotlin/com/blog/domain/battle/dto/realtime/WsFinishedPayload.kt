package com.blog.domain.battle.dto.realtime

import com.blog.domain.battle.entity.BattleEndReason
import com.blog.domain.battle.entity.BattleWinnerTeam

data class WsFinishedPayload(
    val t: String = "FINISHED",
    val matchId: Long,
    val winner: BattleWinnerTeam,
    val reason: BattleEndReason,
    val lane0: Int,
    val lane1: Int,
    val lane2: Int,
    val inputsA: Int,
    val inputsB: Int,
    val extra: Map<String, Any?> = emptyMap(),
)