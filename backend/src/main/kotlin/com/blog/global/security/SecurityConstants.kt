package com.blog.global.security

object SecurityConstants {

    val ALLOWED_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")

    val ALLOWED_HEADERS = listOf(
        "Authorization",
        "Content-Type",
        "Accept",
        "X-Requested-With"
    )

    // 필요하면 프론트에서 읽을 헤더(토큰/리프레시 등) 노출
    val EXPOSED_HEADERS = listOf(
        "Authorization"
    )

    // 인증 없이 접근 가능한 엔드포인트
    val PUBLIC_ENDPOINTS = arrayOf(
        "/",
        "/error",
        "/mcp/**",
        "/favicon.ico",

        // swagger
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs",
        "/v3/api-docs/**",

        // dev only
        "/h2-console/**",

        // actuator
        "/actuator/health", "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus",

        // (프록시로 /api가 붙는 환경 대응)
        "/api/actuator/health", "/api/actuator/health/**",
        "/api/actuator/info",

        // certbot
        "/.well-known/acme-challenge/**",

        // websocket handshake 경로
        "/ws/**",

        "/hikari-status",

        // 회원 기능
        "/api/v1/auth/signup",
        "/api/v1/auth/login"
    )

    // CORS를 적용할 경로
    val CORS_PATHS = arrayOf("/api/**", "/ws/**")
}
