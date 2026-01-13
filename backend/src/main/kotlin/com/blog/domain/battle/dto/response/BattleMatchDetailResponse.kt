package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleTeam

data class BattleMatchDetailResponse(
    val matchId: Long,
    val status: BattleMatchStatus,
    val myTeam: BattleTeam?,
    val lane0: Int,
    val lane1: Int,
    val lane2: Int,
    val endsAtEpochMs: Long?,   // RUNNING일 때만
)