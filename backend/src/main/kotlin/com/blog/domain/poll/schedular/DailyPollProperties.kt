package com.blog.domain.poll.schedular

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "custom.daily-poll")
@ConditionalOnProperty(
    prefix = "custom.daily-poll",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
data class DailyPollProperties(
    val enabled: Boolean = true,
    val cron: String = "0 15 14 * * ?",
    val zone: String = "Asia/Seoul",
    val count: Int = 1,
    val creatorUserId: Long = 1L,
)
