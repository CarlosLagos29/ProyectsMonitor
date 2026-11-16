package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.common.enums.HealthStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class RecentCheckResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-10T14:00:00Z")
    val checkedAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "UP")
    val status: HealthStatus,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Total time, redirects included. About 10000 on a timeout",
        example = "142",
    )
    val latencyMs: Int,
)
