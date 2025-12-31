package com.blog.domain.user.repository

import com.blog.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun existsByUsername(username: String): Boolean

    fun findByUsernameAndDeletedAtIsNull(username: String): User?

    fun findByIdAndDeletedAtIsNull(id: Long): User?
}
