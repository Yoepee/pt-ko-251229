package com.blog.domain.battle.dto.realtime

data class WsStatePayload(
    val t: String = "STATE",
    val matchId: Long,
    val endsAtEpochMs: Long?,
    val lane0: Int,
    val lane1: Int,
    val lane2: Int,
    val sumA: Int,
    val sumB: Int,
    val inputsA: Int,
    val inputsB: Int,
)