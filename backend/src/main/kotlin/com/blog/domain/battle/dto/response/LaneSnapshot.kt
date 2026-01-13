package com.blog.domain.battle.dto.response

data class LaneSnapshot(
    val lane0A: Int,
    val lane1A: Int,
    val lane2A: Int,
    val lane0B: Int,
    val lane1B: Int,
    val lane2B: Int,
    val inputsA: Int,
    val inputsB: Int,
)
