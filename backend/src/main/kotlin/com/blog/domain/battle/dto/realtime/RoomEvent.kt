package com.blog.domain.battle.dto.realtime

data class RoomEvent(
    val type: RoomEventType,
    val matchId: Long,
    val payload: Any? = null,
    val ts: Long = System.currentTimeMillis(),
)
