package com.blog.domain.poll.dto.response

import com.blog.domain.poll.entity.PollType
import java.time.LocalDateTime

data class PollRankingResponse(
    val pollId: Long,
    val title: String,
    val pollType: PollType,
    val categoryId: Long?,
    val endsAt: LocalDateTime?,

    val voteCount: Long,              // (range/track 기준)
    val stats: PollStatsPreview? = null, // totalVotes(전체 누적)
    val preview: PollPreview? = null,
)
