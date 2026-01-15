package com.blog.domain.battle.realtime

import com.blog.domain.battle.dto.request.BattleInputRequest
import com.blog.domain.battle.repository.BattleParticipantJooqRepository
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import com.blog.global.realtime.WsErrorSender
import com.blog.global.realtime.WsSessionRegistry
import com.blog.global.security.JwtWsAuthSupport
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.JsonNode

@Component
class BattleWsHandler(
    private val auth: JwtWsAuthSupport,
    private val registry: WsSessionRegistry,
    private val battleService: BattleJooqService,
    private val objectMapper: ObjectMapper,
    private val wsErrorSender: WsErrorSender,
) : WebSocketHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession): Mono<Void> {

        val matchId = extractMatchId(session)
            ?: return wsErrorSender.sendAndClose(session, 400, "invalid matchId")

        val userId = auth.userIdFromAccessCookie(session)
            ?: return wsErrorSender.sendAndClose(session, 401, "unauthorized")

        val key = "battle:$matchId"

        return Mono.fromRunnable<Void> {
            battleService.assertRoomReadable(userId, matchId)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { e -> wsErrorSender.sendAndClose(session, e) }
            .then(
                Mono.fromRunnable {
                    registry.add(key, userId, session)
                    log.info("WS connected. matchId={}, userId={}", matchId, userId)
                }
            )
            .thenMany(
                session.receive()
                    .map { it.payloadAsText }
                    .flatMap({ payload ->
                        handleMessage(session, userId, matchId, payload)
                    }, 32)
                    .doFinally { signalType ->
                        registry.remove(key, userId)
                        log.info("WS disconnected. matchId={}, userId={}, signal={}", matchId, userId, signalType)

                        battleService.onWsDisconnected(userId, matchId)
                    }
            )
            .then()
    }

    private fun handleMessage(
        session: WebSocketSession,
        userId: Long,
        matchId: Long,
        payload: String
    ): Mono<Void> {
        return Mono.fromCallable {
            objectMapper.readTree(payload)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { node ->
                val messageType = node["t"]?.asText() ?: return@flatMap Mono.empty<Void>()

                when (messageType) {
                    "INPUT" -> handleInput(session, userId, matchId, node)
                    "PING" -> handlePing(session)
                    else -> Mono.empty<Void>()
                }
            }
            .onErrorResume { e ->
                log.error("Message handling error: matchId={}, userId={}", matchId, userId, e)
                wsErrorSender.send(session, e)
            }
    }

    private fun handleInput(
        session: WebSocketSession,
        userId: Long,
        matchId: Long,
        node: JsonNode
    ): Mono<Void> {
        val lane = node["lane"]?.asInt()
        val power = node["power"]?.asInt()

        if (lane == null || power == null) {
            return wsErrorSender.send(
                session,
                ApiException(ErrorCode.BATTLE_INVALID_INPUT)
            )
        }

        return Mono.fromRunnable<Void> {
            battleService.submitInput(
                userId = userId,
                matchId = matchId,
                req = BattleInputRequest(lane = lane, power = power)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { e -> wsErrorSender.send(session, e) }
            .then()
    }

    private fun handlePing(session: WebSocketSession): Mono<Void> {
        return session.send(
            Mono.just(session.textMessage("""{"t":"PONG"}"""))
        ).then()
    }

    private fun extractMatchId(session: WebSocketSession): Long? {
        val path = session.handshakeInfo.uri.path // e.g. /ws/battles/123
        return path.substringAfterLast("/").toLongOrNull()
    }
}