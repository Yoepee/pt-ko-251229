package com.blog.domain.poll.dto.request

import com.blog.domain.poll.entity.PollVisibility
import java.time.LocalDateTime

data class PollUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val categoryId: Long? = null,
    val visibility: PollVisibility? = null,

    val allowAnonymous: Boolean? = null,
    val allowChange: Boolean? = null,
    val endsAt: LocalDateTime? = null,
)
