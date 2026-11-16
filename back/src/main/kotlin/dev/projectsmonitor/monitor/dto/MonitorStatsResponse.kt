package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.common.enums.HealthStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class MonitorStatsResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Start of the window: now minus 24 hours",
        example = "2026-11-09T14:00:00Z",
    )
    val from: Instant,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "End of the window: now",
        example = "2026-11-10T14:00:00Z",
    )
    val to: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Uptime of the window")
    val uptime: UptimeResponse,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Checks per status in the window. Always has the four keys",
        example = """{"UP": 140, "DOWN": 2, "BLOCKED": 0, "REJECTED": 0}""",
    )
    val byStatus: Map<HealthStatus, Long>,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Latency of the UP checks only")
    val latency: LatencyResponse,
)
