package com.blog.domain.battle.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.jvm.javaClass
import com.blog.domain.battle.service.BattleRedisPort
import com.blog.domain.battle.service.BattleJooqService

@Component
class BattleFinishWorker(
    private val redis: BattleRedisPort,
    private val service: BattleJooqService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 500)
    fun tick() {
        val now = System.currentTimeMillis()
        val due = redis.popDueMatches(now, 100)
        if (due.isEmpty()) return

        for (matchId in due) {
            runCatching { service.finishByWorker(matchId) }
                .onFailure { e ->
                    log.warn("Battle finish failed. matchId={}", matchId, e)
                }
        }
    }
}