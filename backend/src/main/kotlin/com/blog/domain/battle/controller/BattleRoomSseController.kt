package com.blog.domain.battle.controller

import com.blog.domain.battle.dto.realtime.RoomEvent
import com.blog.domain.battle.dto.realtime.RoomEventType
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.realtime.RealtimeKeys
import com.blog.global.realtime.RealtimeProperties
import com.blog.global.realtime.SseHub
import com.blog.global.security.JwtPrincipal
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/v1/battles")
class BattleRoomSseController(
    private val sseHub: SseHub,
    private val props: RealtimeProperties,
    private val battleService: BattleJooqService,
) {

    @GetMapping(
        value = ["/rooms/{matchId}/events"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun roomEvents(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
    ): Flux<ServerSentEvent<RoomEvent>> {

        battleService.assertRoomReadable(principal.userId, matchId)

        val key = RealtimeKeys.room(matchId)

        // 접속 즉시 스냅샷 1회 보내기
        val initial = Flux.just(
            RoomEvent(
                type = RoomEventType.ROOM_SNAPSHOT,
                matchId = matchId,
                payload = battleService.getRoomDetailForBroadcast(matchId)
            )
        ).map { ev ->
            ServerSentEvent.builder(ev)
                .event(ev.type.name)
                .id(ev.ts.toString())
                .build()
        }

        val stream = sseHub.streamAs(key, RoomEvent::class.java)
            .map { ev ->
                ServerSentEvent.builder(ev)
                    .event(ev.type.name)
                    .id(ev.ts.toString())
                    .build()
            }

        val keepAlive = Flux.interval(Duration.ofSeconds(props.sse.keepAliveSec))
            .map {
                ServerSentEvent.builder<RoomEvent>()
                    .comment("keep-alive")
                    .build()
            }

        return Flux.merge(initial, stream, keepAlive)
    }
}