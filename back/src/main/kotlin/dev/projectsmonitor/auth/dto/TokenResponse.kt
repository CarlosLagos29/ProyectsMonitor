package dev.projectsmonitor.auth.dto

import dev.projectsmonitor.auth.token.IssuedToken
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class TokenResponse(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "JWT to send as Authorization: Bearer <token> on every other endpoint",
        example = "eyJhbGciOiJIUzI1NiJ9...",
    )
    val token: String,

    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "When the token expires (UTC). There is no refresh token",
        example = "2026-11-11T02:00:00Z",
    )
    val expiresAt: Instant,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "User the token belongs to")
    val user: UserResponse,
) {
    companion object {
        fun from(issued: IssuedToken) = TokenResponse(
            token = issued.token,
            expiresAt = issued.user.expiresAt,
            user = UserResponse.from(issued.user),
        )
    }
}
