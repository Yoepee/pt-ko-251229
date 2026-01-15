package com.blog.domain.battle.realtime

import com.blog.domain.battle.dto.request.BattleInputRequest
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.realtime.WsSessionRegistry
import com.blog.global.security.CookieProperties
import com.blog.global.security.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap

@Component
class BattleWsHandler(
    private val battleService: BattleJooqService,
    private val jwtProvider: JwtProvider,
    private val cookieProps: CookieProperties,
    private val objectMapper: ObjectMapper,
    private val sessionRegistry: WsSessionRegistry
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    // sessionId -> SessionInfo
    private val sessionInfoMap = ConcurrentHashMap<String, SessionInfo>()

    data class SessionInfo(
        val matchId: Long,
        val userId: Long,
        val username: String
    )

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val matchId = extractMatchId(session)
                ?: return closeWithError(session, "Invalid matchId")

            val (userId, username) = extractUserInfo(session)
                ?: return closeWithError(session, "Unauthorized")

            // 권한 체크 (참가자인지 확인)
            battleService.assertRoomReadable(userId, matchId)

            // 세션 정보 저장
            val info = SessionInfo(matchId, userId, username)
            sessionInfoMap[session.id] = info

            // 레지스트리에 등록
            sessionRegistry.addSession(matchId, userId, session)

            // WebSocket 연결 알림 (재접속 시 포기 타이머 취소)
            battleService.onWsConnected(userId, matchId)

            log.info("[WS_CONNECT] matchId={}, userId={}, username={}", matchId, userId, username)

            // 연결 성공 메시지 전송
            sendMessage(session, mapOf(
                "t" to "CONNECTED",
                "matchId" to matchId,
                "userId" to userId
            ))

        } catch (e: Exception) {
            log.error("[WS_CONNECT_ERROR]", e)
            closeWithError(session, e.message ?: "Connection failed")
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val info = sessionInfoMap[session.id] ?: return

        try {
            val node = objectMapper.readTree(message.payload)
            val messageType = node["t"]?.asText() ?: return

            when (messageType) {
                "INPUT" -> handleInput(session, info, node)
                "PING" -> handlePing(session)
                else -> log.warn("[WS_UNKNOWN_TYPE] type={}", messageType)
            }

        } catch (e: Exception) {
            log.error("[WS_MESSAGE_ERROR] matchId={}, userId={}", info.matchId, info.userId, e)
            sendError(session, e.message ?: "Message handling error")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val info = sessionInfoMap.remove(session.id) ?: return

        sessionRegistry.removeSession(info.matchId, info.userId)
        log.info("[WS_DISCONNECT] matchId={}, userId={}, status={}", info.matchId, info.userId, status)

        try {
            // 연결 해제 처리 (포기 체크 등)
            battleService.onWsDisconnected(info.userId, info.matchId)
        } catch (e: Exception) {
            log.error("[WS_DISCONNECT_ERROR]", e)
        }
    }

    // =============== Message Handlers ===============

    private fun handleInput(
        session: WebSocketSession,
        info: SessionInfo,
        node: JsonNode
    ) {
        val lane = node["lane"]?.asInt()
        val power = node["power"]?.asInt()

        if (lane == null || power == null) {
            return sendError(session, "Invalid input: lane and power required")
        }

        battleService.submitInput(
            userId = info.userId,
            matchId = info.matchId,
            req = BattleInputRequest(lane = lane, power = power)
        )
    }

    private fun handlePing(session: WebSocketSession) {
        sendMessage(session, mapOf("t" to "PONG"))
    }

    // =============== Utility Methods ===============

    private fun extractMatchId(session: WebSocketSession): Long? {
        val path = session.uri?.path ?: return null
        return path.substringAfterLast("/").toLongOrNull()
    }

    private fun extractUserInfo(session: WebSocketSession): Pair<Long, String>? {
        val cookies = session.handshakeHeaders["cookie"]?.firstOrNull() ?: return null

        val tokenValue = cookies.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("${cookieProps.accessTokenName}=") }
            ?.substringAfter("=")
            ?: return null

        val principal = jwtProvider.tryParseAccess(tokenValue) ?: return null
        return principal.userId to principal.username
    }

    private fun sendMessage(session: WebSocketSession, payload: Map<String, Any>) {
        try {
            if (!session.isOpen) return
            val json = objectMapper.writeValueAsString(payload)
            session.sendMessage(TextMessage(json))
        } catch (e: Exception) {
            log.error("[WS_SEND_ERROR]", e)
        }
    }

    private fun sendError(session: WebSocketSession, msg: String) {
        sendMessage(session, mapOf(
            "t" to "ERROR",
            "message" to msg
        ))
    }

    private fun closeWithError(session: WebSocketSession, msg: String) {
        try {
            sendError(session, msg)
            Thread.sleep(100) // 메시지 전송 대기
            session.close(CloseStatus.POLICY_VIOLATION)
        } catch (e: Exception) {
            log.error("[WS_CLOSE_ERROR]", e)
        }
    }
}