package com.blog.domain.poll.schedular

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.daily-poll")
data class DailyPollProperties(
    var enabled: Boolean = true,
    var cron: String = "0 15 14 * * ?",
    var zone: String = "Asia/Seoul",
    var count: Int = 1,
    var creatorUserId: Long = 1L,
)
