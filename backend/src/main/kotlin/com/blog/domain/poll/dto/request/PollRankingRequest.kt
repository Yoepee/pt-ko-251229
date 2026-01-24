package com.blog.domain.poll.dto.request

import com.blog.domain.poll.entity.PollType

data class PollRankingRequest(
    val range: RankingRange,
    val track: RankingTrack,
    val type: PollType? = null,
    val categoryId: Long? = null,
)
