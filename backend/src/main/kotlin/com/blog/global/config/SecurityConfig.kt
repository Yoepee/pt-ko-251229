package com.blog.global.config

import com.blog.global.security.SecurityConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain? {
        return http
            .csrf { it.disable() }
            .cors{ }
            .headers { it.frameOptions { it.sameOrigin() } }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin(withDefaults())
            .logout(withDefaults())
            .build()
    }
}