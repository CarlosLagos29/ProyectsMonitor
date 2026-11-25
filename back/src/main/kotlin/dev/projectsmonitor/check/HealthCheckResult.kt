package dev.projectsmonitor.check

import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import java.time.Instant

data class HealthCheckResult(
    val finalUrl: String,
    val status: HealthStatus,
    val httpStatus: Int,
    val latencyMs: Int,
    val redirects: Int,
    val reason: HealthReason,
    val checkedAt: Instant,
)
