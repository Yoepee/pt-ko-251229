package com.blog.domain.poll.schedular

import org.quartz.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@EnableConfigurationProperties(DailyPollProperties::class)
@ConditionalOnProperty(prefix = "custom.daily-poll", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class DailyPollQuartzConfig(
    private val props: DailyPollProperties
) {

    @Bean
    fun dailyPollJobDetail(): JobDetail =
        JobBuilder.newJob(CreateDailyPollJob::class.java)
            .withIdentity(JobKey.jobKey("createDailyPollJob", "poll"))
            .storeDurably()
            .build()

    @Bean
    fun dailyPollTrigger(jobDetail: JobDetail): Trigger =
        TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity(TriggerKey.triggerKey("createDailyPollTrigger", "poll"))
            .withSchedule(
                CronScheduleBuilder.cronSchedule(props.cron)
                    .inTimeZone(TimeZone.getTimeZone(props.zone))
                    .withMisfireHandlingInstructionDoNothing()
            )
            .build()
}