package com.blog.domain.battle.dto.response

data class MyBattleStatsResponse(
    val seasonId: Long,
    val userId: Long,
    val rating: Int,
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
)
