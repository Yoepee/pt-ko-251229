package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode
import java.time.LocalDateTime

data class WaitingRoomRow(
    val matchId: Long,
    val matchType: BattleMatchType,
    val mode: BattleMode,
    val status: BattleMatchStatus,
    val ownerUserId: Long?,
    val activeCount: Int,
    val createdAt: LocalDateTime,
)
