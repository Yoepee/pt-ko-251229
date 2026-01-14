package com.blog.domain.battle.dto.response

import com.blog.domain.battle.entity.BattleMode
import com.blog.domain.battle.entity.BattleTeam
import com.blog.domain.battle.entity.BattleMatchType

data class MyLobbyStateResponse(
    val state: LobbyStateType,
    val matchId: Long? = null,
    val matchType: BattleMatchType? = null,
    val mode: BattleMode? = null,
    val team: BattleTeam? = null,
    val isOwner: Boolean? = null,
)
