package com.blog.domain.battle.dto.request

import jakarta.validation.constraints.NotNull

data class KickRequest(
    @field:NotNull
    val targetUserId: Long
)
