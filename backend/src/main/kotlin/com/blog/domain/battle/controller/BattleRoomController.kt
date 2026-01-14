package com.blog.domain.battle.controller

import com.blog.domain.battle.dto.request.ChangeCharacterRequest
import com.blog.domain.battle.dto.request.ChangeTeamRequest
import com.blog.domain.battle.dto.request.CreateRoomRequest
import com.blog.domain.battle.dto.request.JoinRoomRequest
import com.blog.domain.battle.dto.request.RoomReadyRequest
import com.blog.domain.battle.dto.request.KickRequest
import com.blog.domain.battle.dto.response.BattleRoomDetailResponse
import com.blog.domain.battle.dto.response.BattleRoomSummaryResponse
import com.blog.domain.battle.dto.response.BattleCharacterResponse
import com.blog.domain.battle.service.BattleJooqService
import com.blog.global.common.ApiResponse
import com.blog.global.common.PageResponse
import com.blog.global.security.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/battles")
class BattleRoomController(
    private val battleService: BattleJooqService,
) {
    @PostMapping("/rooms")
    fun createRoom(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: CreateRoomRequest
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val matchId = battleService.createCustomRoom(
            userId = principal.userId,
            req = req
        )
        return ResponseEntity.ok(ApiResponse.created(data = mapOf("matchId" to matchId), message = "방 생성"))
    }

    @GetMapping("/rooms")
    fun listRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<PageResponse<BattleRoomSummaryResponse>>> {
        val res = battleService.listWaitingRooms(page, size)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @GetMapping("/rooms/{matchId}")
    fun getRoomDetail(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
    ): ResponseEntity<ApiResponse<BattleRoomDetailResponse>> {
        val res = battleService.getRoomDetail(principal.userId, matchId)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @PostMapping("/rooms/{matchId}/join")
    fun joinRoom(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: JoinRoomRequest,
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val joinedMatchId = battleService.joinRoom(
            matchId = matchId,
            userId = principal.userId,
            characterId = req.characterId
        )
        return ResponseEntity.ok(ApiResponse.ok(data = mapOf("matchId" to joinedMatchId), message = "방 참가"))
    }

    @PostMapping("/rooms/{matchId}/leave")
    fun leaveRoom(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.leaveMatch(principal.userId, matchId)
        return ResponseEntity.ok(ApiResponse.ok(message = "방 나가기"))
    }

    @GetMapping("/characters")
    fun listCharacters(): ResponseEntity<ApiResponse<List<BattleCharacterResponse>>> {
        val res = battleService.listCharacters()
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @PatchMapping("/rooms/{matchId}/character")
    fun changeCharacter(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: ChangeCharacterRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.changeCharacter(principal.userId, matchId, req.characterId)
        return ResponseEntity.ok(ApiResponse.ok(message = "캐릭터 변경"))
    }

    @PatchMapping("/rooms/{matchId}/ready")
    fun setReady(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: RoomReadyRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.setRoomReady(
            userId = principal.userId,
            matchId = matchId,
            ready = req.ready
        )
        return ResponseEntity.ok(ApiResponse.ok(message = if (req.ready) "준비 완료" else "준비 해제"))
    }

    @PostMapping("/rooms/{matchId}/start")
    fun startRoom(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.startRoom(principal.userId, matchId)
        return ResponseEntity.ok(ApiResponse.ok(message = "게임 시작"))
    }

    @PostMapping("/rooms/{matchId}/kick")
    fun kick(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: KickRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.kickFromRoom(principal.userId, matchId, req.targetUserId)
        return ResponseEntity.ok(ApiResponse.ok(message = "강퇴"))
    }

    @PatchMapping("/rooms/{matchId}/team")
    fun changeTeam(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable matchId: Long,
        @Valid @RequestBody req: ChangeTeamRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        battleService.changeTeam(principal.userId, matchId, req.team)
        return ResponseEntity.ok(ApiResponse.ok(message = "팀 변경"))
    }
}