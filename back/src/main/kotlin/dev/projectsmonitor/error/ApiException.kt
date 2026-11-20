package dev.projectsmonitor.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.time.Duration
import kotlin.math.ceil

open class ApiException(
    val status: HttpStatus,
    val reason: ApiErrorReason,
    override val message: String,
    val retryAfter: Duration? = null,
) : RuntimeException(message)

class InvalidUrlException(message: String) : ApiException(HttpStatus.BAD_REQUEST, ApiErrorReason.INVALID_URL, message)

class RateLimitExceededException(retryAfter: Duration) :
    ApiException(
        HttpStatus.TOO_MANY_REQUESTS,
        ApiErrorReason.RATE_LIMITED,
        "Rate limit exceeded. Try again in ${retryAfter.toRetryAfterSeconds()} seconds.",
        retryAfter,
    )

fun problem(status: HttpStatus, reason: ApiErrorReason, detail: String): ProblemDetail =
    ProblemDetail.forStatusAndDetail(status, detail).apply {
        title = status.reasonPhrase
        setProperty("reason", reason)
    }

fun Duration.toRetryAfterSeconds(): Long = maxOf(1, ceil(toMillis() / 1000.0).toLong())
