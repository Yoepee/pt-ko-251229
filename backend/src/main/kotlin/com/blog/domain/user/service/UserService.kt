package com.blog.domain.user.service

import com.blog.domain.user.entity.User
import com.blog.domain.user.repository.UserRepository
import com.blog.global.exception.ApiException
import com.blog.global.exception.ErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun signUp(username: String, password: String, nickname: String): Long {
        if (userRepository.existsByUsername(username)) {
            throw ApiException(ErrorCode.USERNAME_DUPLICATED)
        }

        val user = User(
            username = username,
            password = passwordEncoder.encode(password)!!,
            nickname = nickname
        )
        return userRepository.save(user).id!!
    }

    @Transactional(readOnly = true)
    fun findByUsername(username: String): User =
        userRepository.findByUsernameAndDeletedAtIsNull(username)
            ?: throw ApiException(ErrorCode.USER_NOT_FOUND)

    @Transactional(readOnly = true)
    fun findById(userId: Long): User =
        userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw ApiException(ErrorCode.USER_NOT_FOUND)

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = findById(userId)

        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw ApiException(ErrorCode.PASSWORD_MISMATCH)
        }
        if (passwordEncoder.matches(newPassword, user.password)) {
            throw ApiException(ErrorCode.SAME_PASSWORD_NOT_ALLOWED)
        }

        user.changePassword(passwordEncoder.encode(newPassword)!!)
    }

    @Transactional
    fun changeNickname(userId: Long, newNickname: String) {
        val user = findById(userId)
        user.changeNickname(newNickname)
    }

    @Transactional
    fun withdraw(userId: Long) {
        val user = findById(userId)
        user.withdraw()
    }
}
