package com.blog.domain.battle.dto.request

import com.blog.domain.battle.entity.BattleTeam
import jakarta.validation.constraints.NotNull

data class ChangeTeamRequest(
    @field:NotNull
    val team: BattleTeam
)
