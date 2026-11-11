package dev.projectsmonitor.error

import org.springframework.http.HttpStatus
import java.time.Duration
import kotlin.math.ceil

open class ApiException(
    val status: HttpStatus,
    val reason: ApiErrorReason,
    override val message: String,
    val retryAfter: Duration? = null,
) : RuntimeException(message)

fun Duration.toRetryAfterSeconds(): Long = maxOf(1, ceil(toMillis() / 1000.0).toLong())
