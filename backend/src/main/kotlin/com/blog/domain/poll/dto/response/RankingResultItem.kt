package com.blog.domain.poll.dto.response

data class RankingResultItem(
    val optionId: Long,
    val label: String,
    val count: Long,
    val percent: Int,
    val rank: Int,
)
