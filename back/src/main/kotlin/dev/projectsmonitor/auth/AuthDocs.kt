package dev.projectsmonitor.auth

import dev.projectsmonitor.auth.dto.MeResponse
import dev.projectsmonitor.auth.dto.TokenResponse
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Hidden
annotation class LoginDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Get a guest token",
    description = "Issues a new guest session valid for 12 hours. Nothing is stored: the guest is the id inside the " +
        "token. Requires Authorization: Bearer <API_TOKEN> (not a JWT). Limited to 10 per hour per client IP, so " +
        "the client should reuse the token until it expires. A new token is a new guest: the monitor created with " +
        "the previous one is no longer reachable.",
)
@ApiResponse(
    responseCode = "200",
    description = "Token issued.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = TokenResponse::class),
            examples = [ExampleObject(name = "Guest", value = GUEST_TOKEN_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing or wrong API token (reason INVALID_API_TOKEN).",
    content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "429",
    description = "Too many guest tokens from this IP (reason RATE_LIMITED). The Retry-After header says how many " +
        "seconds to wait.",
    content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))],
)
annotation class GuestTokenDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Current session",
    description = "Returns the user behind the token, when the token expires and the limits that apply to it, so " +
        "the client does not hardcode them.",
)
@ApiResponse(
    responseCode = "200",
    description = "Current session.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MeResponse::class),
            examples = [
                ExampleObject(name = "Guest", value = ME_GUEST_EXAMPLE),
                ExampleObject(name = "Admin", value = ME_ADMIN_EXAMPLE),
            ],
        ),
    ],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = "application/problem+json", schema = Schema(implementation = ProblemDetail::class))],
)
annotation class MeDocs

private const val GUEST_TOKEN_EXAMPLE = """
{"token": "eyJhbGciOiJIUzI1NiJ9...", "expiresAt": "2026-11-11T02:00:00Z",
 "user": {"id": "0b6c3f52-8f6e-4d7a-9c1e-5a2f7b9d3e41", "role": "GUEST"}}
"""

private const val ME_GUEST_EXAMPLE = """
{"user": {"id": "0b6c3f52-8f6e-4d7a-9c1e-5a2f7b9d3e41", "role": "GUEST"}, "expiresAt": "2026-11-11T02:00:00Z",
 "limits": {"maxMonitors": 1, "canSetPublic": false, "checkIntervalSeconds": 600, "manualCheckCooldownSeconds": 60}}
"""

private const val ME_ADMIN_EXAMPLE = """
{"user": {"id": "admin", "role": "ADMIN"}, "expiresAt": "2026-11-11T02:00:00Z",
 "limits": {"maxMonitors": null, "canSetPublic": true, "checkIntervalSeconds": 600, "manualCheckCooldownSeconds": 60}}
"""
