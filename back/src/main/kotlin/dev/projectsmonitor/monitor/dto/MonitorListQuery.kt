package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

enum class MonitorScope { OWN, PUBLIC, GUESTS }

data class MonitorListQuery(
    @Schema(description = "Zero-based page number", example = "0", defaultValue = "0")
    @Min(value = 0, message = "must be 0 or greater")
    val page: Int = 0,

    @Schema(description = "Monitors per page, 1 to 50", example = "10", defaultValue = "10")
    @Min(value = 1, message = "must be at least 1")
    @Max(value = 50, message = "must be at most 50")
    val size: Int = 10,

    @Schema(
        description = "OWN: the current user's monitors (a guest gets only their own). PUBLIC: the admin's " +
            "public monitors. GUESTS: the active guest monitors, admin only",
        example = "OWN",
        defaultValue = "OWN",
    )
    val scope: MonitorScope = MonitorScope.OWN,
)
