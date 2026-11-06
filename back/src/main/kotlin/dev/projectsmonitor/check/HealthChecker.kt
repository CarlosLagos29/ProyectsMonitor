package dev.projectsmonitor.check

import okhttp3.HttpUrl
import java.time.Duration

interface HealthChecker {
    fun check(target: HttpUrl, timeout: Duration? = null): HealthCheckResult
}
