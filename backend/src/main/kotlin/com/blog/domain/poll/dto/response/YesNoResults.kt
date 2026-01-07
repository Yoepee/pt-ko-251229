package com.blog.domain.poll.dto.response

data class YesNoResults(
    val yesOptionId: Long,
    val noOptionId: Long,
    val yesCount: Long,
    val noCount: Long,
    val yesPercent: Int,
    val noPercent: Int,
) : PollResultsResponse
