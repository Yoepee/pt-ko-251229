package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMode

data class MatchInfo(
    val seasonId: Long,
    val matchType: BattleMatchType,
    val status: BattleMatchStatus,
    val mode: BattleMode,
)
