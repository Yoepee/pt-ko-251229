package com.blog.global.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.jwt")
data class JwtProperties(
    val secret: String,
    val accessExpireSeconds: Long,
    val refreshExpireSeconds: Long
)
