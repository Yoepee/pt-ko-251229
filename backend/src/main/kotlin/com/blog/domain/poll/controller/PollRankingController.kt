package com.blog.domain.poll.controller

import com.blog.domain.poll.dto.request.PollRankingRequest
import com.blog.domain.poll.dto.response.PollRankingResponse
import com.blog.domain.poll.service.PollRankingService
import com.blog.global.common.ApiResponse
import com.blog.global.common.PageResponse
import com.blog.global.security.JwtPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/polls/rankings")
class PollRankingController(
    private val rankingService: PollRankingService,
) {

    @GetMapping
    fun ranking(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        req: PollRankingRequest,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<PollRankingResponse>>> {
        val res = rankingService.getRanking(principal,  req.range, req.track, req.type, req.categoryId, pageable)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }
}