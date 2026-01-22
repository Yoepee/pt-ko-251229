package com.blog.domain.poll.schedular

import com.blog.domain.poll.service.DailyPollService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.stereotype.Component

@Component
@DisallowConcurrentExecution
class CreateDailyPollJob(
    private val dailyPollService: DailyPollService
) : Job {
    override fun execute(context: JobExecutionContext) {
        dailyPollService.createTodayIfAbsent()
    }
}