package dev.projectsmonitor.monitor

import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail

private const val PROBLEM = "application/problem+json"

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "List monitors",
    description = "Admin: all of their monitors. Guest: their own monitor, if any, plus the admin's public ones " +
        "(owned: false, read-only). Each one comes with its current status and the uptime of the last 24 hours. " +
        "Own monitors first, then newest first. Paginated with page (zero-based, default 0) and size (1 to 50, " +
        "default 10).",
)
@ApiResponse(
    responseCode = "200",
    description = "Monitors visible to the current user.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MonitorListResponse::class),
            examples = [ExampleObject(name = "Guest", value = LIST_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "400",
    description = "page or size out of range (reason VALIDATION_ERROR, with errors per field).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "429",
    description = "Too many requests from this guest (reason RATE_LIMITED). See Retry-After.",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class ListMonitorsDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(summary = "Get a monitor", description = "Same shape as each item of the list.")
@ApiResponse(
    responseCode = "200",
    description = "The monitor.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MonitorResponse::class),
            examples = [ExampleObject(name = "Public monitor", value = MONITOR_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "404",
    description = "It does not exist, it expired or it is someone else's private monitor (reason NOT_FOUND).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class GetMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Create a monitor",
    description = "Normalizes the URL and schedules the first check right away. A guest can have one monitor, " +
        "which expires with their token. The URL cannot be changed later: to watch another one, create a new " +
        "monitor and delete the old one.",
)
@ApiResponse(
    responseCode = "201",
    description = "Monitor created. The Location header points to it.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MonitorResponse::class),
            examples = [ExampleObject(name = "Guest monitor", value = CREATED_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid fields (reason VALIDATION_ERROR, with errors) or invalid URL (reason INVALID_URL).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "403",
    description = "A guest sent isPublic: true (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "409",
    description = "The guest already has a monitor (reason MONITOR_LIMIT_REACHED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "429",
    description = "Too many requests from this guest (reason RATE_LIMITED). See Retry-After.",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "503",
    description = "Too many guests have a monitor right now (reason GUEST_CAPACITY_REACHED). Retry-After says when " +
        "the oldest one expires.",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class CreateMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Update a monitor",
    description = "Partial update: only the fields sent change. The URL cannot be changed and is ignored if sent.",
)
@ApiResponse(
    responseCode = "200",
    description = "The updated monitor.",
    content = [Content(mediaType = "application/json", schema = Schema(implementation = MonitorResponse::class))],
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid fields (reason VALIDATION_ERROR, with errors).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "403",
    description = "Someone else's public monitor, or a guest sent isPublic: true (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "404",
    description = "Not found (reason NOT_FOUND).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class UpdateMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Delete a monitor",
    description = "Deletes the monitor and all its history. A guest can then create another one.",
)
@ApiResponse(responseCode = "204", description = "Deleted.")
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "403",
    description = "Someone else's public monitor (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "404",
    description = "Not found (reason NOT_FOUND).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class DeleteMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Check now",
    description = "Checks the monitor right away, stores the result and moves the next scheduled check to 10 " +
        "minutes from now. Takes up to about 10 seconds. A site that is down is still a 200 with status DOWN. " +
        "Once per minute per monitor, and a guest can do it 10 times per hour.",
)
@ApiResponse(
    responseCode = "200",
    description = "Result of the check.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = CheckResultResponse::class),
            examples = [ExampleObject(name = "Up", value = CHECK_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "401",
    description = "Missing, invalid or expired token (reason SESSION_EXPIRED).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "403",
    description = "Someone else's public monitor (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "404",
    description = "Not found (reason NOT_FOUND).",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "429",
    description = "Checked less than a minute ago, the guest's hourly limit, or too many checks to the same site " +
        "(reason RATE_LIMITED). See Retry-After.",
    content = [Content(mediaType = PROBLEM, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class CheckMonitorDocs

annotation class MonitorStatsDocs

annotation class MonitorResultsDocs

private const val MONITOR_EXAMPLE = """
{"id": "6f1c2a9e-3b7d-4c11-9a52-0e8f4d2b7a10", "name": "Portfolio",
 "url": "https://portfoliocarloslagos.netlify.app/", "isPublic": true, "enabled": true, "owned": false,
 "expiresAt": null, "nextCheckAt": "2026-11-10T14:10:00Z",
 "lastCheck": {"checkedAt": "2026-11-10T14:00:00Z", "status": "UP", "reason": "SUCCESS",
  "message": "The site is up and responding", "httpStatus": 200, "latencyMs": 142},
 "uptime": {"percentage": 100.0, "checks": 12},
 "createdAt": "2026-11-10T13:00:00Z", "updatedAt": "2026-11-10T13:00:00Z"}
"""

private const val LIST_EXAMPLE =
    "{\"monitors\": [$MONITOR_EXAMPLE], \"page\": {\"number\": 0, \"size\": 10, \"totalElements\": 1, \"totalPages\": 1}}"

private const val CREATED_EXAMPLE = """
{"id": "0d2e4f61-7a8b-4c9d-8e1f-2a3b4c5d6e7f", "name": "My site", "url": "https://example.com/",
 "isPublic": false, "enabled": true, "owned": true, "expiresAt": "2026-11-11T02:00:00Z",
 "nextCheckAt": "2026-11-10T14:00:00Z", "lastCheck": null, "uptime": {"percentage": null, "checks": 0},
 "createdAt": "2026-11-10T14:00:00Z", "updatedAt": "2026-11-10T14:00:00Z"}
"""

private const val CHECK_EXAMPLE = """
{"checkedAt": "2026-11-10T14:00:00Z", "status": "UP", "reason": "SUCCESS",
 "message": "The site is up and responding", "httpStatus": 200, "latencyMs": 142,
 "finalUrl": "https://portfoliocarloslagos.netlify.app/", "redirects": 0}
"""
