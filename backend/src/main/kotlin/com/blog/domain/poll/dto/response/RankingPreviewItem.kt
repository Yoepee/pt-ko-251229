package com.blog.domain.poll.dto.response

data class RankingPreviewItem(
    val optionId: Long,
    val label: String,
    val count: Long,
    val percent: Int,
    val rank: Int,
)