package com.blog.domain.poll.dto.response

data class PollOptionResponse(
    val id: Long,
    val text: String,
    val sortOrder: Int,
)