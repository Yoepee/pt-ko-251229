package com.blog.domain.poll.dto.response

data class YesNoPreview(
    val yesCount: Long,
    val noCount: Long,
    val yesPercent: Int, // 0~100
    val noPercent: Int,  // 0~100 (합 100 보장)
) : PollPreview