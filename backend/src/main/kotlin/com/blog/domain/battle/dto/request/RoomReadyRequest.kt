package com.blog.domain.battle.dto.request

import jakarta.validation.constraints.NotNull

data class RoomReadyRequest(
    @field:NotNull
    val ready: Boolean
)
