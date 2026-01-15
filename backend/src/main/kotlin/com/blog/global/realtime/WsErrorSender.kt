package com.blog.global.realtime

import com.blog.global.common.ApiResponse
import com.blog.global.exception.ApiException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

@Component
class WsErrorSender(
    private val objectMapper: ObjectMapper
) {
    fun send(session: WebSocketSession, e: Throwable): Mono<Void> {
        val (status, message) = when (e) {
            is ApiException -> e.status to e.message
            else -> 500 to (e.message ?: "UNKNOWN")
        }

        val json = objectMapper.writeValueAsString(ApiResponse.fail(status, message))
        return session.send(Mono.just(session.textMessage(json))).then()
    }

    fun sendAndClose(session: WebSocketSession, e: Throwable): Mono<Void> =
        send(session, e).onErrorResume { Mono.empty<Void>() }
            .then(session.close())

    fun sendAndClose(session: WebSocketSession, status: Int, message: String): Mono<Void> {
        val json = objectMapper.writeValueAsString(ApiResponse.fail(status, message))
        return session.send(Mono.just(session.textMessage(json)))
            .onErrorResume { Mono.empty<Void>() }
            .then()
            .then(session.close())
    }
}