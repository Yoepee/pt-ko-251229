package com.blog.domain.poll.service

import com.blog.domain.poll.dto.request.RankingRange
import com.blog.domain.poll.dto.request.RankingTrack
import com.blog.domain.poll.dto.response.PollPreview
import com.blog.domain.poll.dto.response.PollRankingResponse
import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.repository.PollJooqRepository
import com.blog.domain.poll.repository.PollRankingJooqRepository
import com.blog.global.common.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.blog.domain.poll.dto.response.PollStatsPreview
import com.blog.domain.poll.service.mapper.toRankingPreview
import com.blog.domain.poll.service.mapper.toYesNoPreview
import com.blog.global.security.JwtPrincipal
import java.time.LocalDateTime
import kotlin.math.ceil

@Service
class PollRankingService(
    private val rankingRepo: PollRankingJooqRepository,
    private val pollJooqRepository: PollJooqRepository,
) {

    @Transactional(readOnly = true)
    fun getRanking(
        principal: JwtPrincipal?,
        range: RankingRange,
        track: RankingTrack,
        pollType: PollType?,
        categoryId: Long?,
        pageable: Pageable,
    ): PageResponse<PollRankingResponse> {

        val offset = pageable.pageNumber * pageable.pageSize
        val limit = pageable.pageSize

        val total = rankingRepo.countDistinctPolls(range, track, pollType, categoryId)
        val content = rankingRepo.rank(range, track, pollType, categoryId, offset, limit)

        if (content.isEmpty()) {
            val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / limit.toDouble()).toInt()
            val hasNext = (offset + limit) < total

            return PageResponse(
                content = emptyList(),
                page = pageable.pageNumber,
                size = pageable.pageSize,
                totalElements = total,
                totalPages = totalPages,
                hasNext = hasNext,
            )
        }

        val pollIds = content.map { it.pollId }

        val totalVotesMap = pollJooqRepository.fetchTotalVotes(pollIds)
        val yesNoMap = pollJooqRepository.fetchYesNoCounts(pollIds)
        val rankMap = pollJooqRepository.fetchRankingTop(pollIds, topN = 5)

        val myVotesMap: Map<Long, List<Long>> =
            if (principal != null) pollJooqRepository.fetchMyVoteOptionIdsMap(pollIds, principal.userId)
            else emptyMap()

        val now = LocalDateTime.now()

        val enriched = content.map { item ->
            val totalVotes = totalVotesMap[item.pollId] ?: 0L

            val myVoteOptionIds: List<Long>? =
                if (principal != null) (myVotesMap[item.pollId] ?: emptyList())
                else null

            val stats = PollStatsPreview(
                totalVotes = totalVotes,
                myVoteOptionIds = myVoteOptionIds,
                endsInSeconds = item.endsAt?.let { java.time.Duration.between(now, it).seconds.coerceAtLeast(0) }
            )

            val preview: PollPreview? = when (item.pollType) {
                PollType.VOTE -> yesNoMap[item.pollId]?.toYesNoPreview()
                PollType.RANK -> rankMap[item.pollId]?.toRankingPreview(topN = 5)
            }

            item.copy(stats = stats, preview = preview)
        }

        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / limit.toDouble()).toInt()
        val hasNext = (offset + limit) < total

        return PageResponse(enriched, pageable.pageNumber, pageable.pageSize, total, totalPages, hasNext)
    }
}