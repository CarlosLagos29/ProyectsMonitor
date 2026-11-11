package dev.projectsmonitor.auth.dto

import dev.projectsmonitor.auth.Role
import dev.projectsmonitor.config.AppProperties
import io.swagger.v3.oas.annotations.media.Schema

data class LimitsResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
        description = "Maximum number of monitors, null when unlimited",
        example = "1",
    )
    val maxMonitors: Int?,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Whether isPublic can be sent",
        example = "false",
    )
    val canSetPublic: Boolean,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Seconds between two scheduled checks of a monitor",
        example = "600",
    )
    val checkIntervalSeconds: Long,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Minimum wait between two manual checks of the same monitor",
        example = "60",
    )
    val manualCheckCooldownSeconds: Long,
)
