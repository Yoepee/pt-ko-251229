package com.blog.domain.user.dto.response

data class MeResponse(
    val id: Long,
    val username: String,
    val nickname: String,
    val role: String
)