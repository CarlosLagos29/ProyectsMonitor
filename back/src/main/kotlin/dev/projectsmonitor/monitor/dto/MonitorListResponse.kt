package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class MonitorListResponse(
    @ArraySchema(
        arraySchema = Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Monitors of the requested page. Own monitors first, then newest first",
        ),
    )
    val monitors: List<MonitorResponse>,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Pagination of the list")
    val page: PageResponse,
)
