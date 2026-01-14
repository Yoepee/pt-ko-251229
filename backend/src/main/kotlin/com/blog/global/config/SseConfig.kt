package com.blog.global.config

import com.blog.global.realtime.RealtimeProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RealtimeProperties::class)
class SseConfig {
}