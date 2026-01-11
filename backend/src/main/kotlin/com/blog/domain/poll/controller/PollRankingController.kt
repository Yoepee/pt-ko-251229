package com.blog.domain.poll.controller

import com.blog.domain.poll.dto.request.RankingRange
import com.blog.domain.poll.dto.request.RankingTrack
import com.blog.domain.poll.dto.response.PollRankingResponse
import com.blog.domain.poll.entity.PollType
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/polls/rankings")
class PollRankingController(
    private val rankingService: PollRankingService,
) {

    @GetMapping
    fun ranking(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @RequestParam range: RankingRange,
        @RequestParam track: RankingTrack,
        @RequestParam(required = false) pollType: PollType?,
        @RequestParam(required = false) categoryId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<PollRankingResponse>>> {
        val res = rankingService.getRanking(principal, range, track, pollType, categoryId, pageable)
        return ResponseEntity.ok(ApiResponse.ok(data = res))
    }
}