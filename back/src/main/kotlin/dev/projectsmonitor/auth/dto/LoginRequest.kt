package dev.projectsmonitor.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Admin email", example = "admin@example.com")
    @NotBlank(message = "must not be blank")
    val email: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Admin password in plain text (sent over HTTPS)",
        example = "Example",
    )
    @NotBlank(message = "must not be blank")
    val password: String,
)
