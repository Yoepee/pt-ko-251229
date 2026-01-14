package com.blog.domain.battle.dto.realtime

data class LobbyEvent(
    val type: LobbyEventType,
    val payload: Any? = null,
    val ts: Long = System.currentTimeMillis()
)
