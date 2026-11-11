package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.monitor.MonitorView
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class MonitorListResponse(
    @ArraySchema(
        arraySchema = Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Own monitors first, then by name",
        ),
    )
    val monitors: List<MonitorResponse>,
)
