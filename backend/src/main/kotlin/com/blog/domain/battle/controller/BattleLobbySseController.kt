package com.blog.domain.battle.controller

import com.blog.domain.battle.dto.realtime.LobbyEvent
import com.blog.domain.battle.dto.realtime.LobbyEventType
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.realtime.RealtimeProperties
import com.blog.global.realtime.SseHub
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import com.blog.global.realtime.RealtimeKeys

@RestController
@RequestMapping("/api/v1/battles")
class BattleLobbySseController(
    private val sseHub: SseHub,
    private val props: RealtimeProperties,
    private val battleService: BattleJooqService,
) {
    @GetMapping(
        value = ["/lobby/events"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun lobbyEvents(
        @RequestParam(defaultValue = "20") size: Int,
    ): Flux<ServerSentEvent<LobbyEvent>> {

        val safeSize = size.coerceIn(1, 100)
        val key = RealtimeKeys.lobby()

        // ✅ 접속 즉시 스냅샷
        val initialSnapshot = battleService.listWaitingRooms(page = 0, size = safeSize)

        val initial = Flux.just(
            LobbyEvent(
                type = LobbyEventType.LOBBY_SNAPSHOT,
                payload = initialSnapshot
            )
        ).map { ev ->
            ServerSentEvent.builder(ev)
                .event(ev.type.name)
                .id(ev.ts.toString())
                .build()
        }

        val stream = sseHub.streamAs(key, LobbyEvent::class.java)
            .map { ev ->
                ServerSentEvent.builder(ev)
                    .event(ev.type.name)
                    .id(ev.ts.toString())
                    .build()
            }

        val keepAlive = Flux.interval(Duration.ofSeconds(props.sse.keepAliveSec))
            .map {
                ServerSentEvent.builder<LobbyEvent>()
                    .comment("keep-alive")
                    .build()
            }

        return Flux.merge(initial, stream, keepAlive)
    }
}