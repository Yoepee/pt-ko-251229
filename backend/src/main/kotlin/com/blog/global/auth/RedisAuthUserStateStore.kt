package com.blog.global.auth

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisAuthUserStateStore(
    private val redis: StringRedisTemplate,
) : AuthUserStateStore {
    private fun key(userId: Long) = "auth:user:$userId"
    private val VER = "ver"
    private val STATUS = "status"

    override fun getVersion(userId: Long): Long {
        val v = redis.opsForHash<String, String>().get(key(userId), VER)
        return v?.toLongOrNull() ?: 0L
    }

    override fun getStatus(userId: Long): AuthUserStatus {
        val s = redis.opsForHash<String, String>().get(key(userId), STATUS)
        return runCatching { AuthUserStatus.valueOf(s ?: "ACTIVE") }.getOrDefault(AuthUserStatus.ACTIVE)
    }

    override fun bumpVersion(userId: Long): Long {
        val newVer = redis.opsForHash<String, String>().increment(key(userId), VER, 1L) ?: 1L
        // status가 없으면 기본 ACTIVE 세팅
        redis.opsForHash<String, String>().putIfAbsent(key(userId), STATUS, AuthUserStatus.ACTIVE.name)
        return newVer
    }

    override fun setStatus(userId: Long, status: AuthUserStatus) {
        redis.opsForHash<String, String>().put(key(userId), STATUS, status.name)
        // ver 필드도 없으면 기본 0 세팅
        redis.opsForHash<String, String>().putIfAbsent(key(userId), VER, "0")
    }

    override fun markDeletedAndBump(userId: Long) {
        setStatus(userId, AuthUserStatus.DELETED)
        bumpVersion(userId) // 기존 토큰 즉시 무효화
    }
}