package com.blog.domain.battle.dto.response

data class WaitingRoomsPage(
    val rows: List<WaitingRoomRow>,
    val total: Long
)
