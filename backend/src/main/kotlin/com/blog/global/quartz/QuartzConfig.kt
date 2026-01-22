package com.blog.global.quartz

import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SpringBeanJobFactory

@Configuration
@ConditionalOnProperty(
    prefix = "spring.quartz",
    name = ["job-store-type"],
    havingValue = "jdbc"
)
class QuartzConfig {

    @Bean
    fun jobFactory(beanFactory: AutowireCapableBeanFactory): JobFactory {
        return object : SpringBeanJobFactory() {
            override fun createJobInstance(bundle: TriggerFiredBundle): Any {
                val job = super.createJobInstance(bundle)
                beanFactory.autowireBean(job)
                return job
            }
        }
    }
}