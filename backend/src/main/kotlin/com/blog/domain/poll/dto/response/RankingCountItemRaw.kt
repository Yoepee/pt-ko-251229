package com.blog.domain.poll.dto.response

data class RankingCountItemRaw(
    val optionId: Long,
    val label: String,
    val count: Long,
)
