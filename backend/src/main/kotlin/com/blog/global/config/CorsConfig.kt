package com.blog.global.config

import com.blog.global.security.SecurityConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(
    @Value("\${custom.cors.allowed-origins:http://localhost:3000}")
    private val allowedOriginsProp: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = allowedOriginsProp
                .split(",")
                .map(String::trim)
                .filter(String::isNotBlank)

            allowedMethods = SecurityConstants.ALLOWED_METHODS
            allowedHeaders = SecurityConstants.ALLOWED_HEADERS
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            // 여러 경로에 동일한 CORS 정책 적용
            SecurityConstants.CORS_PATHS.forEach { path ->
                registerCorsConfiguration(path, configuration)
            }
        }
    }
}