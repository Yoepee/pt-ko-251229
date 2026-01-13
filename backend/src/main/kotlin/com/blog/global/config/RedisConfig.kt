package com.blog.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript

@Configuration
class RedisConfig {

    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(connectionFactory)

    @Bean
    fun battleInputLuaScript(): DefaultRedisScript<List<*>> {
        val script = DefaultRedisScript<List<*>>()
        script.setScriptText(
            """
            -- KEYS[1] = rateKey
            -- KEYS[2] = lanesKey
            -- ARGV[1] = limit
            -- ARGV[2] = ttlSec
            -- ARGV[3] = laneField
            -- ARGV[4] = power
            -- ARGV[5] = teamInputsField

            local c = redis.call("INCR", KEYS[1])
            if c == 1 then
              redis.call("EXPIRE", KEYS[1], tonumber(ARGV[2]))
            end
            if c > tonumber(ARGV[1]) then
              return {0, c}
            end

            redis.call("HINCRBY", KEYS[2], ARGV[3], tonumber(ARGV[4]))
            redis.call("HINCRBY", KEYS[2], ARGV[5], 1)

            return {1, c}
            """.trimIndent()
        )
        return script
    }
}