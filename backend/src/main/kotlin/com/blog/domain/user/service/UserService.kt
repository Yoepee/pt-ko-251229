package com.blog.domain.user.service

import com.blog.domain.user.entity.User
import com.blog.domain.user.repository.UserRepository
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService (
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
    ) {

    @Transactional
    fun singUp(username: String, password: String, nickname: String): Long {
        require(!userRepository.existsByUsername(username)){
            "이미 존재하는 사용자입니다."
        }

        val encodedPassword: String =
            requireNotNull(passwordEncoder.encode(password)) { "허용되지 않는 비밀번호 입니다." }
        val user = User(
            username = username,
            password = encodedPassword,
            nickname = nickname
        )
        return userRepository.save(user).id!!
    }

    @Transactional(readOnly = true)
    fun findByUsername(username: String): User =
        userRepository.findByUsername(username).orElseThrow {
            ApiException(ErrorCode.USER_NOT_FOUND)
    }

    fun findById(userId: Long): User =
        userRepository.findById(userId).orElseThrow {
            ApiException(ErrorCode.USER_NOT_FOUND)
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { ApiException(ErrorCode.USER_NOT_FOUND) }

        // ✅ 현재 비밀번호 검증
        val matches = passwordEncoder.matches(currentPassword, user.password)
        if (!matches) {
            throw ApiException(ErrorCode.PASSWORD_MISMATCH)
        }

        if (passwordEncoder.matches(newPassword, user.password)) {
            throw ApiException(ErrorCode.SAME_PASSWORD_NOT_ALLOWED)
        }

        val encodedPassword: String =
            requireNotNull(passwordEncoder.encode(newPassword)) { "허용되지 않는 비밀번호 입니다." }
        user.changePassword(encodedPassword)
    }

    fun changeNickname(userId: Long, newNickname: String) {
        val user = findById(userId)
        user.changeNickname(newNickname)
        userRepository.save(user)
    }
}