package com.blog.domain.battle.dto.request

import com.blog.domain.battle.entity.BattleMode
import jakarta.validation.constraints.NotNull

data class CreateRoomRequest(
    @field:NotNull
    val mode: BattleMode,
    val characterId: Long? = null,
)
