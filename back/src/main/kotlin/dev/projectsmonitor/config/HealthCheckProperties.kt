package dev.projectsmonitor.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.check")
data class HealthCheckProperties(
    val timeouts: Timeouts,
    val maxRedirects: Int,
    val allowedPorts: Set<Int>,
    val perHost: Limit,
) {
    data class Timeouts(val connect: Duration, val read: Duration, val total: Duration)

    data class Limit(val capacity: Long, val period: Duration)
}
