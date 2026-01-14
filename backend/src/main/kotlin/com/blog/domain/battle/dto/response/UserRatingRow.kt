package com.blog.domain.battle.dto.response

data class UserRatingRow(
    val rating: Int,
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
)
