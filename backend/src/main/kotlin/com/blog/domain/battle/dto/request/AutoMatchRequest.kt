package com.blog.domain.battle.dto.request

import com.blog.domain.battle.entity.BattleMatchType
import com.blog.domain.battle.entity.BattleMode
import jakarta.validation.constraints.NotNull

data class AutoMatchRequest(
    @field:NotNull
    val matchType: BattleMatchType = BattleMatchType.RANKED,

    @field:NotNull
    val mode: BattleMode,

    val characterId: Long? = null,
)
