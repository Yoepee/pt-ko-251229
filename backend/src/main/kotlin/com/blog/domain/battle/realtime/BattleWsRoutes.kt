package com.blog.domain.battle.realtime

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class BattleWsRoutes(
    private val battleWsHandler: BattleWsHandler
) {
    @Bean
    fun battleWsMapping(): HandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/battles/{matchId}" to battleWsHandler), -1)
}