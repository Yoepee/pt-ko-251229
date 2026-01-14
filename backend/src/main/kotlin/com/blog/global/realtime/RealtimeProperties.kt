package com.blog.global.realtime

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "realtime")
data class RealtimeProperties(
    val sse: Sse = Sse(),
) {
    data class Sse(
        val keepAliveSec: Long = 15,
        val bufferSize: Int = 10_000,
    )
}
