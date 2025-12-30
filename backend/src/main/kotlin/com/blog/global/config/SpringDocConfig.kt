package com.blog.global.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "블로그 API 서버",
        version = "beta",
        description = "블로그 API 서버 문서입니다."
    ),
    security = [SecurityRequirement(name = "cookieAuth")]
)
@SecurityScheme(
    name = "cookieAuth",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = "accessToken",
    description = "로그인 시 발급된 액세스토큰 쿠키 사용"
)
class SpringDocConfig {
    @Bean
    fun groupApiV1(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("apiV1")
            .pathsToMatch("/api/v1/**")
            .build()

    @Bean
    fun groupController(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("non-api")
            .pathsToExclude("/api/")
            .pathsToMatch("/**")
            .build()
}