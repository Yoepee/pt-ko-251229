package com.blog.domain.user.service

import com.blog.domain.user.dto.response.MeResponse
import com.blog.global.auth.AuthUserStateStore
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
    private val jwtProps: JwtProperties,
    private val userStateStore: AuthUserStateStore,
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val refreshJti: String
    )

    @Transactional
    fun login(username: String, password: String): TokenPair {
        val user = userService.findByUsername(username)

        if (!passwordEncoder.matches(password, user.password)) {
            throw ApiException(ErrorCode.LOGIN_FAILED)
        }

        val userId = user.id!!

        val ver = userStateStore.getVersion(userId)

        val access = jwtProvider.createAccessToken(
            userId = userId,
            username = user.username,
            roles = listOf(user.role.name),
            tokenVersion = ver,
        )

        val (refresh, refreshJti) = jwtProvider.createRefreshToken(
            userId = userId,
            username = user.username,
            roles = listOf(user.role.name),
            tokenVersion = ver,
        )

        refreshTokenStore.save(refreshJti, userId, jwtProps.refreshExpireSeconds)

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

    @Transactional
    fun withdraw(userId: Long, refresh: String?) {
        userService.withdraw(userId)
        userStateStore.bumpVersion(userId)

        if (!refresh.isNullOrBlank()) {
            runCatching {
                val parsed = jwtProvider.parseRefresh(refresh)
                refreshTokenStore.delete(parsed.jti)
            }
        }
    }
}
