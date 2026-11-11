package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.monitor.Uptime
import io.swagger.v3.oas.annotations.media.Schema

data class UptimeResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
        description = "Percentage of UP checks in the last 24 hours, 0 to 100 with one decimal. null without checks",
        example = "98.6",
    )
    val percentage: Double?,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Checks in the last 24 hours", example = "142")
    val checks: Long,
)
