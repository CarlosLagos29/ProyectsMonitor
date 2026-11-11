package dev.projectsmonitor.auth.dto

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.auth.Role
import io.swagger.v3.oas.annotations.media.Schema

data class UserResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "\"admin\" for the admin, a random UUID for each guest",
        example = "0b6c3f52-8f6e-4d7a-9c1e-5a2f7b9d3e41",
    )
    val id: String,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Role of the user", example = "GUEST")
    val role: Role,
)
