package com.blog.domain.poll.dto.response

data class RankingPreview(
    val items: List<RankingPreviewItem>, // 최대 5개
    val etcPercent: Int? = null,          // 0이면 null로 내려도 됨
    val etcCount: Long? = null
) : PollPreview
