package com.blog.global.config

import com.blog.domain.battle.realtime.BattleWsHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val battleWsHandler: BattleWsHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(battleWsHandler, "/ws/battles/{matchId}")
            .setAllowedOriginPatterns("*") // 프로덕션에서는 CORS 설정 필요
    }
}