package com.blog.global.auth

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenStore(private val redis: StringRedisTemplate) {
    fun save(jti: String, userId: Long, ttlSec: Long) {
        redis.opsForValue().set("refresh:$jti", userId.toString(), Duration.ofSeconds(ttlSec))
    }

    fun exists(jti: String): Boolean = redis.hasKey("refresh:$jti")

    fun delete(jti: String) { redis.delete("refresh:$jti") }
}