package dev.projectsmonitor.monitor

import dev.projectsmonitor.common.GuestRateLimitDoc
import dev.projectsmonitor.common.PROBLEM_JSON
import dev.projectsmonitor.common.SessionExpiredDoc
import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.CheckResultsResponse
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import dev.projectsmonitor.monitor.dto.MonitorStatsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ProblemDetail

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "404",
    description = "It does not exist, it expired or it is someone else's private monitor (reason NOT_FOUND).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class MonitorNotFoundDoc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "List monitors",
    description = "One list per scope. OWN (default): the admin gets all of their monitors, public and private; " +
        "a guest gets only their own monitor, if they created it and it has not expired. PUBLIC: the admin's " +
        "public monitors (owned: true for the admin, false and read-only for a guest). GUESTS, admin only: the " +
        "active guest monitors (owned: false), which the admin can manage. Each one comes with its current status, " +
        "the uptime of the last 24 hours and its last 24 checks (recentChecks) for the sparkline. Newest first. " +
        "Paginated with page (zero-based, default 0) and size (1 to 50, default 10); totalElements counts only " +
        "the requested scope.",
)
@ApiResponse(
    responseCode = "200",
    description = "Monitors visible to the current user.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MonitorListResponse::class),
            examples = [ExampleObject(name = "Guest, scope PUBLIC", value = LIST_EXAMPLE)],
        ),
    ],
)
@ApiResponse(
    responseCode = "400",
    description = "page, size or scope invalid (reason VALIDATION_ERROR, with errors per field).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@SessionExpiredDoc
@ApiResponse(
    responseCode = "403",
    description = "A guest sent scope GUESTS (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@GuestRateLimitDoc
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
@SessionExpiredDoc
@MonitorNotFoundDoc
@GuestRateLimitDoc
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
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@SessionExpiredDoc
@ApiResponse(
    responseCode = "403",
    description = "A guest sent isPublic: true (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@ApiResponse(
    responseCode = "409",
    description = "The guest already has a monitor (reason MONITOR_LIMIT_REACHED).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@GuestRateLimitDoc
@ApiResponse(
    responseCode = "503",
    description = "Too many guests have a monitor right now (reason GUEST_CAPACITY_REACHED). Retry-After says when " +
        "the oldest one expires.",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
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
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@SessionExpiredDoc
@ApiResponse(
    responseCode = "403",
    description = "A guest on someone else's public monitor, a guest sent isPublic: true, or the admin sent " +
        "isPublic: true on a guest monitor (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@MonitorNotFoundDoc
@GuestRateLimitDoc
annotation class UpdateMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Delete a monitor",
    description = "Deletes the monitor and all its history. A guest can then create another one.",
)
@ApiResponse(responseCode = "204", description = "Deleted.")
@SessionExpiredDoc
@ApiResponse(
    responseCode = "403",
    description = "A guest on someone else's public monitor (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@MonitorNotFoundDoc
@GuestRateLimitDoc
annotation class DeleteMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Check now",
    description = "Checks the monitor right away, stores the result and moves the next scheduled check to 10 " +
        "minutes after checkedAt. Takes up to about 10 seconds. If the same URL was checked in the last 9 minutes, " +
        "that result is reused and checkedAt tells when it was taken. A site that is down is still a 200 with " +
        "status DOWN. " +
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
@SessionExpiredDoc
@ApiResponse(
    responseCode = "403",
    description = "A guest on someone else's public monitor (reason FORBIDDEN_FOR_ROLE).",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
@MonitorNotFoundDoc
@ApiResponse(
    responseCode = "429",
    description = "Checked less than a minute ago, the guest's hourly limit, or too many checks to the same site " +
        "(reason RATE_LIMITED). See Retry-After.",
    content = [Content(mediaType = PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
)
annotation class CheckMonitorDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Monitor stats",
    description = "Summary of the last 24 hours: uptime, checks per status and latency. The latency only counts UP " +
        "checks, since a timeout is not a latency. Same access as the monitor detail.",
)
@ApiResponse(
    responseCode = "200",
    description = "Stats of the last 24 hours.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = MonitorStatsResponse::class),
            examples = [ExampleObject(name = "Mostly up", value = STATS_EXAMPLE)],
        ),
    ],
)
@SessionExpiredDoc
@MonitorNotFoundDoc
@GuestRateLimitDoc
annotation class MonitorStatsDocs

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "Monitor results",
    description = "Every check of the last 24 hours, oldest first, for the latency chart and the status line. " +
        "About 144 points plus the manual checks, with gaps while the server was asleep. Same access as the " +
        "monitor detail.",
)
@ApiResponse(
    responseCode = "200",
    description = "Checks of the last 24 hours.",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = CheckResultsResponse::class),
            examples = [ExampleObject(name = "Two checks", value = RESULTS_EXAMPLE)],
        ),
    ],
)
@SessionExpiredDoc
@MonitorNotFoundDoc
@GuestRateLimitDoc
annotation class MonitorResultsDocs

private const val MONITOR_EXAMPLE = """
{"id": "6f1c2a9e-3b7d-4c11-9a52-0e8f4d2b7a10", "name": "Portfolio",
 "url": "https://portfoliocarloslagos.netlify.app/", "isPublic": true, "enabled": true, "owned": false,
 "expiresAt": null, "nextCheckAt": "2026-11-10T14:10:00Z",
 "lastCheck": {"checkedAt": "2026-11-10T14:00:00Z", "status": "UP", "reason": "SUCCESS",
  "message": "The site is up and responding", "httpStatus": 200, "latencyMs": 142},
 "uptime": {"percentage": 100.0, "checks": 12},
 "recentChecks": [{"checkedAt": "2026-11-10T13:50:00Z", "status": "UP", "latencyMs": 156},
  {"checkedAt": "2026-11-10T14:00:00Z", "status": "UP", "latencyMs": 142}],
 "createdAt": "2026-11-10T13:00:00Z", "updatedAt": "2026-11-10T13:00:00Z"}
"""

private const val LIST_EXAMPLE =
    "{\"monitors\": [$MONITOR_EXAMPLE], \"page\": {\"number\": 0, \"size\": 10, \"totalElements\": 1, \"totalPages\": 1}}"

private const val CREATED_EXAMPLE = """
{"id": "0d2e4f61-7a8b-4c9d-8e1f-2a3b4c5d6e7f", "name": "My site", "url": "https://example.com/",
 "isPublic": false, "enabled": true, "owned": true, "expiresAt": "2026-11-11T02:00:00Z",
 "nextCheckAt": "2026-11-10T14:00:00Z", "lastCheck": null, "uptime": {"percentage": null, "checks": 0},
 "recentChecks": [], "createdAt": "2026-11-10T14:00:00Z", "updatedAt": "2026-11-10T14:00:00Z"}
"""

private const val CHECK_EXAMPLE = """
{"checkedAt": "2026-11-10T14:00:00Z", "status": "UP", "reason": "SUCCESS",
 "message": "The site is up and responding", "httpStatus": 200, "latencyMs": 142,
 "finalUrl": "https://portfoliocarloslagos.netlify.app/", "redirects": 0}
"""

private const val STATS_EXAMPLE = """
{"from": "2026-11-09T14:00:00Z", "to": "2026-11-10T14:00:00Z", "uptime": {"percentage": 98.6, "checks": 142},
 "byStatus": {"UP": 140, "DOWN": 2, "BLOCKED": 0, "REJECTED": 0}, "latency": {"avgMs": 180, "p95Ms": 410}}
"""

private const val RESULTS_EXAMPLE = """
{"from": "2026-11-09T14:00:00Z", "to": "2026-11-10T14:00:00Z", "results": [
 {"checkedAt": "2026-11-10T13:50:00Z", "status": "UP", "reason": "SUCCESS",
  "message": "The site is up and responding", "httpStatus": 200, "latencyMs": 142,
  "finalUrl": "https://portfoliocarloslagos.netlify.app/", "redirects": 0},
 {"checkedAt": "2026-11-10T14:00:00Z", "status": "DOWN", "reason": "TIMEOUT",
  "message": "The site took too long to respond", "httpStatus": 0, "latencyMs": 10003,
  "finalUrl": "https://portfoliocarloslagos.netlify.app/", "redirects": 0}]}
"""
