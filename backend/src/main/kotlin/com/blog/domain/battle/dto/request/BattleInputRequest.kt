package com.blog.domain.battle.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class BattleInputRequest(
    @field:NotNull
    @field:Min(0)
    @field:Max(2)
    val lane: Int,

    // 확장 대비 (기본 1)
    @field:Min(1)
    @field:Max(10)
    val power: Int = 1
)
