package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleTeam

data class BattleRoomParticipantResponse(
    val userId: Long,
    val team: BattleTeam,
    val characterId: Long,
    val characterName: String?,
    val rating: Int?,
    val wins: Int?,
    val losses: Int?,
    val draws: Int?,
    val isOwner: Boolean,
    val isReady: Boolean,
)
