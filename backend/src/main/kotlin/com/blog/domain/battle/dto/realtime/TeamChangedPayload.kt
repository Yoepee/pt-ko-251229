package com.blog.domain.battle.dto.realtime

import com.blog.domain.battle.entity.BattleTeam

data class TeamChangedPayload(val userId: Long, val team: BattleTeam)
