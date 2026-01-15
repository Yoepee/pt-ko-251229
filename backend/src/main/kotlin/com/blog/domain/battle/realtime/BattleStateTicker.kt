package com.blog.domain.battle.realtime

import com.blog.domain.battle.service.BattleJooqService
import com.blog.domain.battle.service.BattleRedisPort
import com.blog.global.realtime.WsSessionRegistry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class BattleStateTicker(
    private val battleService: BattleJooqService,
    private val battleRedis: BattleRedisPort,
    private val registry: WsSessionRegistry,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var disposable: Disposable

    @PostConstruct
    fun start() {
        disposable = Flux.interval(Duration.ofMillis(50))
            .flatMap { tickOnce() }
            .onErrorContinue { e, _ -> log.warn("ticker error", e) }
            .subscribe()
    }

    @PreDestroy
    fun stop() {
        disposable.dispose()
    }

    private fun tickOnce(): Mono<Void> {
        return Mono.fromCallable { battleRedis.listRunningMatches(200) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany { Flux.fromIterable(it) }
            .flatMap({ matchId ->
                // ✅ 1) forfeit 체크 먼저
                Mono.fromRunnable<Void> {
                    battleService.checkAndFinishForfeitIfNeeded(matchId)
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(broadcastStateIfAny(matchId))
            }, 32)
            .then()
    }

    private fun broadcastStateIfAny(matchId: Long): Mono<Void> {
        val key = "battle:$matchId"
        if (registry.connectedCount(key) == 0) return Mono.empty()

        return Mono.fromCallable {
            val snap = battleRedis.getLaneSnapshot(matchId)
            val endsAt = battleRedis.getEndsAt(matchId)

            val payload = mapOf(
                "t" to "STATE",
                "matchId" to matchId,
                "endsAtEpochMs" to endsAt,
                "lane0" to (snap.lane0A + snap.lane0B),
                "lane1" to (snap.lane1A + snap.lane1B),
                "lane2" to (snap.lane2A + snap.lane2B),
                "sumA" to (snap.lane0A + snap.lane1A + snap.lane2A),
                "sumB" to (snap.lane0B + snap.lane1B + snap.lane2B),
                "inputsA" to snap.inputsA,
                "inputsB" to snap.inputsB,
            )
            objectMapper.writeValueAsString(payload)
        }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { json -> registry.broadcast(key, json) }
    }
}