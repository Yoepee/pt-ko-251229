package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleTeam

data class AutoMatchResponse(
    val matchId: Long,
    val status: BattleMatchStatus,
    val team: BattleTeam
)
