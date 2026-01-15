package com.blog.global.realtime

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class WsSessionRegistry {

    private val sessions =
        ConcurrentHashMap<String, ConcurrentHashMap<Long, WebSocketSession>>() // key -> (userId -> session)

    fun add(key: String, userId: Long, session: WebSocketSession) {
        sessions.computeIfAbsent(key) { ConcurrentHashMap() }[userId] = session
    }

    fun remove(key: String, userId: Long) {
        sessions[key]?.remove(userId)
        if (sessions[key]?.isEmpty() == true) sessions.remove(key)
    }

    fun isConnected(key: String, userId: Long): Boolean =
        sessions[key]?.containsKey(userId) == true

    fun connectedCount(key: String): Int =
        sessions[key]?.size ?: 0

    fun broadcast(key: String, json: String): Mono<Void> {
        val map = sessions[key] ?: return Mono.empty()
        val list = map.values.toList()
        if (list.isEmpty()) return Mono.empty()

        return Flux.fromIterable(list)
            .flatMap { s ->
                if (s.isOpen) {
                    s.send(Mono.just(s.textMessage(json)))
                } else {
                    Mono.fromRunnable {
                        map.entries.removeIf { it.value == s }
                    }
                }
            }
            .then()
    }
}