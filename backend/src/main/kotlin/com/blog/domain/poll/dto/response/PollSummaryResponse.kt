package com.blog.domain.poll.dto.response

import com.blog.domain.poll.entity.PollType
import com.blog.domain.poll.entity.PollVisibility
import java.time.LocalDateTime

data class PollSummaryResponse(
    val id: Long,
    val title: String,
    val pollType: PollType,
    val visibility: PollVisibility,
    val categoryId: Long?,
    val endsAt: LocalDateTime?,
    val createdAt: LocalDateTime?,

    val stats: PollStatsPreview? = null,
    val preview: PollPreview? = null,
)
