package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateMonitorRequest(
    @Schema(
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "New display name, 1 to 60 characters",
        example = "Portfolio (prod)",
    )
    @Size(max = 60, message = "must be at most 60 characters")
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val name: String? = null,

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Admin only", example = "true")
    val isPublic: Boolean? = null,

    @Schema(
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        description = "false pauses the scheduled checks",
        example = "false",
    )
    val enabled: Boolean? = null,
)
