package com.blog.domain.battle.dto.realtime

import com.blog.domain.battle.entity.BattleMatchStatus

data class MatchStartedPayload(val status: BattleMatchStatus = BattleMatchStatus.RUNNING)
