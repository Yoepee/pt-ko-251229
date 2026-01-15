package com.blog.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig {
    @Bean
    fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()
}