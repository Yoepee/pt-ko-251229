package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode

data class BattleRoomSummaryResponse(
    val matchId: Long,
    val matchType: BattleMatchType,
    val mode: BattleMode,
    val status: BattleMatchStatus,
    val ownerUserId: Long?,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val createdAtEpochMs: Long,
)
