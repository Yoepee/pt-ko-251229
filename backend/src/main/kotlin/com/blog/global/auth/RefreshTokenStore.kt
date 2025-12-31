package com.blog.global.auth

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenStore(private val redis: StringRedisTemplate) {

    fun save(jti: String, userId: Long, ttlSec: Long) {
        redis.opsForValue().set(key(jti), userId.toString(), Duration.ofSeconds(ttlSec))
    }

    fun exists(jti: String): Boolean =
        redis.hasKey(key(jti)) == true

    fun delete(jti: String) {
        redis.delete(key(jti))
    }

    fun rotate(oldJti: String, newJti: String, userId: Long, ttlSec: Long) {
        delete(oldJti)
        save(newJti, userId, ttlSec)
    }

    private fun key(jti: String) = "refresh:$jti"
}