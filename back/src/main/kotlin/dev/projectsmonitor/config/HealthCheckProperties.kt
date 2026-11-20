package dev.projectsmonitor.config

import dev.projectsmonitor.ratelimit.RateLimit
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app.check")
class HealthCheckProperties(
    val timeouts: Timeouts,
    val maxRedirects: Int,
    val allowedPorts: Set<Int>,
    val perHost: RateLimit,
    val resultTtl: Duration,
) {
    class Timeouts(val connect: Duration, val read: Duration, val total: Duration)
}
