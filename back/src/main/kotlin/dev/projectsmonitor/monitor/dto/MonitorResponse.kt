package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.monitor.MonitorView
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class MonitorResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "6f1c2a9e-3b7d-4c11-9a52-0e8f4d2b7a10")
    val id: UUID,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Portfolio")
    val name: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Normalized URL. It cannot be changed",
        example = "https://portfoliocarloslagos.netlify.app/",
    )
    val url: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Whether guests can see it. Always false for guest monitors",
        example = "true",
    )
    val isPublic: Boolean,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "false = scheduled checks paused",
        example = "true",
    )
    val enabled: Boolean,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "true if it belongs to the current user. false = someone else's public monitor, read-only",
        example = "true",
    )
    val owned: Boolean,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
        description = "When it is deleted. null for admin monitors",
        example = "2026-11-11T02:00:00Z",
    )
    val expiresAt: Instant?,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "When the next scheduled check is due. Can be in the past if the server was asleep",
        example = "2026-11-10T14:10:00Z",
    )
    val nextCheckAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, description = "null until the first check")
    val lastCheck: LastCheckResponse?,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Last 24 hours")
    val uptime: UptimeResponse,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-10T13:00:00Z")
    val createdAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-10T13:00:00Z")
    val updatedAt: Instant,
)
