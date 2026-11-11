package dev.projectsmonitor.monitor.dto

import dev.projectsmonitor.check.toMessage
import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import dev.projectsmonitor.monitor.CheckResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CheckResultResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-10T14:00:00Z")
    val checkedAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "UP")
    val status: HealthStatus,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "SUCCESS")
    val reason: HealthReason,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "The site is up and responding")
    val message: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "HTTP status of the site, 0 when it never answered",
        example = "200",
    )
    val httpStatus: Int,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Total time, redirects included",
        example = "142",
    )
    val latencyMs: Int,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Last URL reached",
        example = "https://portfoliocarloslagos.netlify.app/",
    )
    val finalUrl: String,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Redirects followed", example = "0")
    val redirects: Int,
)
