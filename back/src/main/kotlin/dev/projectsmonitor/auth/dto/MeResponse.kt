package dev.projectsmonitor.auth.dto

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.config.AppProperties
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class MeResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Current user")
    val user: UserResponse,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "When the current token expires (UTC)",
        example = "2026-11-11T02:00:00Z",
    )
    val expiresAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Limits of the user's role")
    val limits: LimitsResponse,
) {
    companion object {
        fun from(user: CurrentUser, limits: AppProperties.Limits) = MeResponse(
            user = UserResponse.from(user),
            expiresAt = user.expiresAt,
            limits = LimitsResponse.from(user.role, limits),
        )
    }
}
