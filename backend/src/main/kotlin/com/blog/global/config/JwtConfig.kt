package com.blog.global.config

import com.blog.global.security.CookieProperties
import com.blog.global.security.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class, CookieProperties::class)
class JwtConfig {
}