package com.blog.global.config

import com.blog.global.common.ApiResponse
import com.blog.global.security.JwtAuthFilter
import com.blog.global.security.SecurityConstants
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.json.JsonMapper

@Configuration
class SecurityConfig(
    private val jsonMapper: JsonMapper
) {
    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { it.frameOptions { it.sameOrigin() } }
            .exceptionHandling { eh ->
                eh.authenticationEntryPoint { _, res, _ ->
                    writeJson(res, 401, ApiResponse.fail(401, "로그인 후 이용해주세요."))
                }
                eh.accessDeniedHandler { _, res, _ ->
                    writeJson(res, 403, ApiResponse.fail(403, "권한이 없습니다."))
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/polls", "/api/v1/polls/**", "/api/v1/polls/rankings").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/polls/*/votes").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    private fun writeJson(res: HttpServletResponse, status: Int, body: Any) {
        res.status = status
        res.characterEncoding = "UTF-8"
        res.contentType = MediaType.APPLICATION_JSON_VALUE
        res.writer.write(jsonMapper.writeValueAsString(body))
    }
}