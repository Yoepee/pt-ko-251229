package com.blog.domain.battle.dto.response

data class BattleCharacterResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val versionNo: Int,
)
