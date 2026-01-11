package com.blog.domain.poll.dto.response

data class YesNoCountRaw(
    val pollId: Long,
    val yesCount: Long,
    val noCount: Long,
    val total: Long,
)