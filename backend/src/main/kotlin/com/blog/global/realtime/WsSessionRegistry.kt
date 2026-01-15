package com.blog.global.realtime

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap


@Component
class WsSessionRegistry(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // matchId -> (userId -> session)
    private val matchSessions = ConcurrentHashMap<Long, ConcurrentHashMap<Long, WebSocketSession>>()

    fun addSession(matchId: Long, userId: Long, session: WebSocketSession) {
        matchSessions.computeIfAbsent(matchId) { ConcurrentHashMap() }[userId] = session
        log.debug("[REGISTRY_ADD] matchId={}, userId={}, totalUsers={}",
            matchId, userId, matchSessions[matchId]?.size)
    }

    fun removeSession(matchId: Long, userId: Long) {
        matchSessions[matchId]?.remove(userId)

        // 빈 방 정리
        if (matchSessions[matchId]?.isEmpty() == true) {
            matchSessions.remove(matchId)
            log.debug("[REGISTRY_CLEANUP] matchId={} removed (no users)", matchId)
        }
    }

    fun isUserConnected(matchId: Long, userId: Long): Boolean =
        matchSessions[matchId]?.containsKey(userId) == true

    fun getConnectedUserCount(matchId: Long): Int =
        matchSessions[matchId]?.size ?: 0

    fun getConnectedUserIds(matchId: Long): Set<Long> =
        matchSessions[matchId]?.keys ?: emptySet()

    /**
     * 특정 매치의 모든 사용자에게 메시지 브로드캐스트
     */
    fun broadcast(matchId: Long, payload: Map<String, Any>) {
        val sessions = matchSessions[matchId] ?: return
        if (sessions.isEmpty()) return

        val json = try {
            objectMapper.writeValueAsString(payload)
        } catch (e: Exception) {
            log.error("[BROADCAST_JSON_ERROR]", e)
            return
        }

        broadcastJson(matchId, json)
    }

    /**
     * JSON 문자열 직접 브로드캐스트 (성능 최적화)
     */
    fun broadcastJson(matchId: Long, json: String) {
        val sessions = matchSessions[matchId] ?: return
        val deadSessions = mutableListOf<Long>()

        sessions.forEach { (userId, session) ->
            try {
                if (session.isOpen) {
                    session.sendMessage(TextMessage(json))
                } else {
                    deadSessions.add(userId)
                }
            } catch (e: Exception) {
                log.warn("[BROADCAST_ERROR] matchId={}, userId={}", matchId, userId, e)
                deadSessions.add(userId)
            }
        }

        // 죽은 세션 정리
        deadSessions.forEach { userId ->
            sessions.remove(userId)
        }
    }

    /**
     * 특정 사용자에게만 메시지 전송
     */
    fun sendToUser(matchId: Long, userId: Long, payload: Map<String, Any>) {
        val session = matchSessions[matchId]?.get(userId)
        if (session == null || !session.isOpen) return

        try {
            val json = objectMapper.writeValueAsString(payload)
            session.sendMessage(TextMessage(json))
        } catch (e: Exception) {
            log.error("[SEND_TO_USER_ERROR] matchId={}, userId={}", matchId, userId, e)
        }
    }

    /**
     * 전체 활성 매치 목록 (모니터링용)
     */
    fun getActiveMatches(): Set<Long> = matchSessions.keys.toSet()

    /**
     * 통계 정보
     */
    fun getStats(): Map<String, Any> = mapOf(
        "activeMatches" to matchSessions.size,
        "totalConnections" to matchSessions.values.sumOf { it.size },
        "matchDetails" to matchSessions.map { (matchId, sessions) ->
            mapOf(
                "matchId" to matchId,
                "users" to sessions.size
            )
        }
    )
}