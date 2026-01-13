package com.blog.domain.battle.entity

data class BattleCharacterRow(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val versionNo: Int,
)