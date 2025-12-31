package com.blog.domain.user.service

import com.blog.domain.user.dto.response.MeResponse
import com.blog.global.auth.RefreshTokenStore
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import com.blog.global.security.JwtProperties
import com.blog.global.security.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val jwtProps: JwtProperties
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val refreshJti: String
    )

    @Transactional(readOnly = true)
    fun login(username: String, password: String): TokenPair {
        val user = userService.findByUsername(username)
        if (!passwordEncoder.matches(password, user.password)) {
            throw ApiException(ErrorCode.LOGIN_FAILED)
        }

        val access = jwtProvider.createAccessToken(
            userId = user.id!!,
            username = user.username,
            roles = listOf(user.role.name)
        )

        // refreshToken + jti
        val (refresh, refreshJti) = jwtProvider.createRefreshToken(
            userId = user.id!!,
            username = user.username,
            roles = listOf(user.role.name)
        )

        refreshTokenStore.save(refreshJti, user.id!!, jwtProps.refreshExpireSeconds)

        return TokenPair(access, refresh, refreshJti)
    }

    @Transactional(readOnly = true)
    fun me(userId: Long): MeResponse {
        val user = userService.findById(userId)
        return MeResponse(
            id = user.id!!,
            username = user.username,
            nickname = user.nickname,
            role = user.role.name
        )
    }
}