package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMode
import com.blog.domain.battle.entity.BattleTeam
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType

data class MyActiveMatchRow(
    val matchId: Long,
    val status: BattleMatchStatus,
    val matchType: BattleMatchType,
    val mode: BattleMode,
    val team: BattleTeam,
    val ownerUserId: Long?,
)
