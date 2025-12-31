package com.blog.global.config

import com.blog.global.security.JwtAuthFilter
import com.blog.global.security.SecurityConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain? {
        return http
            .csrf { it.disable() }
            .cors{ }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { it.frameOptions { it.sameOrigin() } }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .logout{ it.disable() }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}