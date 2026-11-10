package dev.projectsmonitor.check

import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import java.time.Instant

const val NO_HTTP_STATUS = 0

data class HealthCheckResult(
    val url: String,
    val finalUrl: String,
    val status: HealthStatus,
    val httpStatus: Int = NO_HTTP_STATUS,
    val latencyMs: Long,
    val redirects: Int,
    val reason: HealthReason,
    val checkedAt: Instant,
)
