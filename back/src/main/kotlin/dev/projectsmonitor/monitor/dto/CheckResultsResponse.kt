package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CheckResultsResponse(
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

    @ArraySchema(
        arraySchema = Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Checks of the window, oldest first. Can have gaps while the server was asleep",
        ),
    )
    val results: List<CheckResultResponse>,
)
