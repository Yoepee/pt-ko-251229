package com.blog.global.realtime

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class SseHub(
    private val props: RealtimeProperties
) {
    private val sinks = ConcurrentHashMap<String, Sinks.Many<Any>>()

    private fun sink(key: String): Sinks.Many<Any> =
        sinks.computeIfAbsent(key) {
            Sinks.many().multicast().onBackpressureBuffer(props.sse.bufferSize, false)
        }

    fun publish(key: String, event: Any) {
        sink(key).tryEmitNext(event)
    }

    fun stream(key: String): Flux<Any> =
        sink(key).asFlux()

    fun <T : Any> streamAs(key: String, type: Class<T>): reactor.core.publisher.Flux<T> =
        stream(key).ofType(type)
}