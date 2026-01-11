package com.blog.domain.poll.dto.response

data class OptionCountRow(
    val optionId: Long,
    val label: String,
    val sortOrder: Int,
    val count: Long,
)
