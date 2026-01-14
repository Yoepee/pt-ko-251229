package com.blog.domain.battle.controller

import com.blog.domain.battle.dto.response.MyBattleStatsResponse
import com.blog.domain.battle.dto.response.MyLobbyStateResponse
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/battles/me")
class BattleMeController(
    private val battleService: BattleJooqService,
) {
    @GetMapping("/stats")
    fun myStats(@AuthenticationPrincipal p: JwtPrincipal)
            : ResponseEntity<ApiResponse<MyBattleStatsResponse>> {
        val res = battleService.getMyStats(p.userId)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @GetMapping("/state")
    fun myState(@AuthenticationPrincipal p: JwtPrincipal)
            : ResponseEntity<ApiResponse<MyLobbyStateResponse>> {
        val res = battleService.getMyLobbyState(p.userId)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }
}