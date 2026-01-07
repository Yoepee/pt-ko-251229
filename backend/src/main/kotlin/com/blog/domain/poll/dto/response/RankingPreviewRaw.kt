package com.blog.domain.poll.dto.response

data class RankingPreviewRaw(
    val pollId: Long,
    val total: Long,
    val top: List<RankingCountItemRaw>,
)