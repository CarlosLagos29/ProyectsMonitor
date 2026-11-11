package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateMonitorRequest(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Display name, 1 to 60 characters",
        example = "Portfolio",
    )
    @NotBlank(message = "must not be blank")
    @Size(max = 60, message = "must be at most 60 characters")
    val name: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "URL to check. https:// is assumed when no scheme is given. Only http/https on ports 80 and " +
            "443. It cannot be changed later",
        example = "portfoliocarloslagos.netlify.app",
    )
    @NotBlank(message = "must not be blank")
    @Size(max = 2048, message = "must be at most 2048 characters")
    val url: String,

    @Schema(
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "Whether guests can see it. Admin only, defaults to false",
        example = "false",
    )
    val isPublic: Boolean? = null,
)
