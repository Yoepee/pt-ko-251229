package com.blog.domain.battle.controller

import com.blog.domain.battle.dto.request.AutoMatchRequest
import com.blog.domain.battle.dto.request.BattleInputRequest
import com.blog.domain.battle.dto.response.AutoMatchResponse
import com.blog.domain.battle.dto.response.BattleMatchDetailResponse
import com.blog.domain.battle.entity.BattleMatchStatus
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/battles")
class BattleMatchController(
    private val battleService: BattleJooqService,
) {

    @PostMapping("/auto-match")
    fun autoMatch(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: AutoMatchRequest
    ): ResponseEntity<ApiResponse<AutoMatchResponse>> {

        val res = battleService.autoMatch(
            userId = principal.userId,
            req = req
        )

        val message = when (res.status) {
            BattleMatchStatus.RUNNING -> "매칭 완료"
            BattleMatchStatus.WAITING -> "매칭 대기"
            BattleMatchStatus.FINISHED -> "이미 종료된 매치"
            BattleMatchStatus.CANCELED -> "취소된 매치"
        }

        return ResponseEntity.ok(ApiResponse.ok(data = res, message = message))
    }

    @PostMapping("/{matchId}/inputs")
    fun submitInput(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: BattleInputRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.submitInput(
            userId = principal.userId,
            matchId = matchId,
            req = req
        )
        return ResponseEntity.ok(ApiResponse.ok(message = "입력 처리"))
    }

    @GetMapping("/{matchId}")
    fun getMatchDetail(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
    ): ResponseEntity<ApiResponse<BattleMatchDetailResponse>> {
        val res = battleService.getMatchDetail(principal.userId, matchId)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    // 테스트 전용 종료가 필요하면 "prod에서 막기"를 강하게 추천
    // @Profile("local") 또는 별도 테스트 컨트롤러로 분리
//    @PostMapping("/{matchId}/finish")
//    fun finishForTest(
//        @AuthenticationPrincipal principal: JwtPrincipal,
//        @PathVariable matchId: Long
//    ): ResponseEntity<ApiResponse<Unit>> {
//        battleService.finishForTest(principal.userId, matchId)
//        return ResponseEntity.ok(ApiResponse.ok(message = "종료 처리"))
//    }
}