package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode

data class BattleRoomSnapshotResponse(
    val matchId: Long,
    val matchType: BattleMatchType,
    val mode: BattleMode,
    val status: BattleMatchStatus,
    val ownerUserId: Long?,
    val maxPlayers: Int,
    val participants: List<BattleRoomParticipantResponse>,
)
