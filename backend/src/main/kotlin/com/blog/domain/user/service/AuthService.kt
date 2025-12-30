package com.blog.domain.user.service

import com.blog.domain.user.dto.MeResponse
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import com.blog.global.security.JwtProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider
) {
    @Transactional(readOnly = true)
    fun login(username: String, password: String): String {
        val user = userService.findByUsername(username)
        if (!passwordEncoder.matches(password, user.password)) {
            throw ApiException(ErrorCode.LOGIN_FAILED)
        }
        return jwtProvider.createAccessToken(
            userId = user.id!!,
            username = user.username,
            roles = listOf(user.role.name)
        )
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