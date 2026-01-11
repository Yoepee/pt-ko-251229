package com.blog.domain.poll.dto.response

import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.entity.PollVisibility
import java.time.LocalDateTime

data class PollDetailResponse(
    val id: Long,
    val creatorUserId: Long,
    val title: String,
    val description: String?,
    val pollType: PollType,
    val visibility: PollVisibility,
    val categoryId: Long?,
    val allowAnonymous: Boolean,
    val allowChange: Boolean,
    val maxSelections: Int,
    val endsAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val options: List<PollOptionResponse>,

    val stats: PollStatsPreview? = null,
    val results: PollResultsResponse? = null,
)
