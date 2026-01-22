package com.blog

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
@EnableAutoConfiguration(exclude = [QuartzAutoConfiguration::class])
class BackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}