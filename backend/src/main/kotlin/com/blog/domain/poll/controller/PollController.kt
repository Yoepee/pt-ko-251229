package com.blog.domain.poll.controller

import com.blog.domain.poll.dto.request.PollCreateRequest
import com.blog.domain.poll.dto.request.PollListRequest
import com.blog.domain.poll.dto.request.PollUpdateRequest
import com.blog.domain.poll.dto.request.VoteRequest
import com.blog.domain.poll.dto.response.PollCreateResponse
import com.blog.domain.poll.dto.response.PollDetailResponse
import com.blog.domain.poll.dto.response.PollSummaryResponse
import com.blog.domain.poll.service.PollService
import com.blog.global.common.ApiResponse
import com.blog.global.common.PageResponse
import com.blog.global.security.JwtPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest

@RestController
@RequestMapping("/api/v1/polls")
class PollController(
    private val pollService: PollService,
) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @Valid @RequestBody req: PollCreateRequest,
    ): ResponseEntity<ApiResponse<PollCreateResponse>> {
        val res = pollService.create(principal.userId, req)
        return ResponseEntity.status(201).body(ApiResponse.created(data = res, message = "투표 생성 완료"))
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        req: PollListRequest, // query param 바인딩
        @PageableDefault(size = 20, sort = ["id"]) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<PollSummaryResponse>>> {
        val res = pollService.listPublic(principal, req, pageable)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @GetMapping("/{pollId}")
    fun detail(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PathVariable pollId: Long,
    ): ResponseEntity<ApiResponse<PollDetailResponse>> {
        val res = pollService.detail(principal, pollId)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }

    @PatchMapping("/{pollId}")
    fun update(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable pollId: Long,
        @Valid @RequestBody req: PollUpdateRequest,
    ): ResponseEntity<ApiResponse<PollDetailResponse>> {
        val res = pollService.update(principal.userId, pollId, req)
        return ResponseEntity.ok(ApiResponse.ok(data = res, message = "투표 수정 완료"))
    }

    @DeleteMapping("/{pollId}")
    fun delete(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable pollId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        pollService.delete(principal.userId, pollId)
        return ResponseEntity.ok(ApiResponse.ok(message = "투표 삭제 완료"))
    }

    @PostMapping("/{pollId}/votes")
    fun vote(
        @PathVariable pollId: Long,
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @Valid @RequestBody req: VoteRequest,
        httpReq: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        if (principal != null) {
            pollService.voteAsUser(pollId, principal.userId, req)
        } else {
            val anonKey = deriveAnonymousKey(httpReq)
            pollService.voteAsAnonymous(pollId, anonKey, req)
        }
        return ResponseEntity.ok(ApiResponse.ok(message = "투표 완료"))
    }

    private fun deriveAnonymousKey(req: HttpServletRequest): String {
        val ip = req.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: req.remoteAddr
            ?: "unknown"
        val ua = req.getHeader("User-Agent") ?: "unknown"
        val raw = "$ip|$ua"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(64)
    }
}