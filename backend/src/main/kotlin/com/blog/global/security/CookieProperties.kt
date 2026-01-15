package com.blog.global.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.cookie")
data class CookieProperties(
    val secure: Boolean = false,
    val sameSite: String = "Lax",
    val accessTokenName: String = "access_token",
    val refreshTokenName: String = "refresh_token",
)