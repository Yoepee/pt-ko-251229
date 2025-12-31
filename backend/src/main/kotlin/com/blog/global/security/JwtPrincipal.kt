package com.blog.global.security

data class JwtPrincipal (
    val userId: Long,
    val username: String,
    val roles: List<String> = emptyList(),
    val tokenVersion: Long = 0L,
)
