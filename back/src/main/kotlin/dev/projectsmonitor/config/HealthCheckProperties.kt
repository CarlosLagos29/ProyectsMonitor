package dev.projectsmonitor.config

import dev.projectsmonitor.ratelimit.RateLimit
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.check")
data class HealthCheckProperties(
    val timeouts: Timeouts,
    val maxRedirects: Int,
    val allowedPorts: Set<Int>,
    val perHost: RateLimit,
) {
    data class Timeouts(val connect: Duration, val read: Duration, val total: Duration)
}
