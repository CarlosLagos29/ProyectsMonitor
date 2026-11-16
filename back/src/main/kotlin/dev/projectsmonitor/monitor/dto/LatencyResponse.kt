package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.Schema

data class LatencyResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
        description = "Average latency of the UP checks, rounded. null without UP checks",
        example = "180",
    )
    val avgMs: Int?,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
        description = "95th percentile latency of the UP checks. null without UP checks",
        example = "410",
    )
    val p95Ms: Int?,
)
