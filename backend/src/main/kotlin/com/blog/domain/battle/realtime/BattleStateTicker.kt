package com.blog.domain.battle.realtime

import com.blog.domain.battle.service.BattleJooqService
import com.blog.domain.battle.service.BattleRedisPort
import com.blog.global.realtime.WsSessionRegistry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Component
class BattleStateTicker(
    private val battleService: BattleJooqService,
    private val battleRedis: BattleRedisPort,
    private val sessionRegistry: WsSessionRegistry,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var scheduler: ScheduledExecutorService

    @PostConstruct
    fun start() {
        scheduler = Executors.newScheduledThreadPool(2) // 병렬 처리용

        // 50ms마다 상태 브로드캐스트
        scheduler.scheduleAtFixedRate(
            ::broadcastTick,
            0,
            100,
            TimeUnit.MILLISECONDS
        )

        // 1초마다 forfeit 체크 (덜 자주 해도 됨)
        scheduler.scheduleAtFixedRate(
            ::forfeitCheckTick,
            0,
            1000,
            TimeUnit.MILLISECONDS
        )

        log.info("[TICKER_START] interval=50ms")
    }

    @PreDestroy
    fun stop() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
        log.info("[TICKER_STOP]")
    }

    private fun broadcastTick() {
        try {
            // 활성 매치만 조회 (연결된 세션이 있는 것만)
            val activeMatches = sessionRegistry.getActiveMatches()

            if (activeMatches.isEmpty()) return

            activeMatches.forEach { matchId ->
                try {
                    broadcastState(matchId)
                } catch (e: Exception) {
                    log.warn("[BROADCAST_ERROR] matchId={}", matchId, e)
                }
            }

        } catch (e: Exception) {
            log.error("[TICK_ERROR]", e)
        }
    }

    private fun forfeitCheckTick() {
        try {
            val runningMatches = battleRedis.listRunningMatches(200)

            runningMatches.forEach { matchId ->
                try {
                    battleService.checkAndFinishForfeitIfNeeded(matchId)
                } catch (e: Exception) {
                    log.warn("[FORFEIT_CHECK_ERROR] matchId={}", matchId, e)
                }
            }

        } catch (e: Exception) {
            log.error("[FORFEIT_TICK_ERROR]", e)
        }
    }

    private fun broadcastState(matchId: Long) {
        // 연결된 사용자가 없으면 스킵
        if (sessionRegistry.getConnectedUserCount(matchId) == 0) return

        try {
            val snap = battleRedis.getLaneSnapshot(matchId)
            val endsAt = battleRedis.getEndsAt(matchId)

            // JSON을 한 번만 생성 (모든 클라이언트가 같은 데이터 받음)
            val payload = mapOf(
                "t" to "STATE",
                "matchId" to matchId,
                "endsAtEpochMs" to endsAt,
                "lanes" to mapOf(
                    "0" to (snap.lane0A + snap.lane0B),
                    "1" to (snap.lane1A + snap.lane1B),
                    "2" to (snap.lane2A + snap.lane2B)
                ),
                "scores" to mapOf(
                    "teamA" to (snap.lane0A + snap.lane1A + snap.lane2A),
                    "teamB" to (snap.lane0B + snap.lane1B + snap.lane2B)
                ),
                "inputs" to mapOf(
                    "teamA" to snap.inputsA,
                    "teamB" to snap.inputsB
                )
            )

            val json = objectMapper.writeValueAsString(payload)
            sessionRegistry.broadcastJson(matchId, json)

        } catch (e: Exception) {
            log.warn("[STATE_BROADCAST_ERROR] matchId={}", matchId, e)
        }
    }
}