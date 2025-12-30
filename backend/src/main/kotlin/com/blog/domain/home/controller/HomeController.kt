package com.blog.domain.home.controller

import com.zaxxer.hikari.HikariDataSource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress

@RestController
@Tag(name = "HomeController", description = "홈 컨트롤러")
class HomeController(private val dataSource: HikariDataSource) {
    @GetMapping("/")
    fun mainPage(): String {
        val localHost = InetAddress.getLocalHost();
        return """
            <h1>API 서버</h1>
            <p>Host Name: ${localHost.hostName}</p>
            <p>Host Address: ${localHost.hostAddress}</p>
            <div>
                <a href="/swagger-ui/index.html">API 문서로 이동</a>
            </div>
        """.trimIndent()
    }

    @Profile("dev")
    @GetMapping("/hikari-status")
    @Operation(summary = "HikariCP 풀 상태 (dev only)")
    fun hikariStatus(): Map<String, Any> {
        val pool = dataSource.hikariPoolMXBean
        return mapOf(
            "total" to pool.totalConnections,
            "active" to pool.activeConnections,
            "idle" to pool.idleConnections,
            "waiting" to pool.threadsAwaitingConnection
        )
    }
}