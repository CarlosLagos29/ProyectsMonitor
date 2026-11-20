package dev.projectsmonitor.check

import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import dev.projectsmonitor.ssrf.RejectedAddressException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import java.time.Instant
import javax.net.ssl.SSLException

object HealthCheckMapper {

    private const val NO_RESPONSE = 0

    fun fromResponse(followed: FollowedResponse, latencyMs: Long, checkedAt: Instant): HealthCheckResult {
        val reason = reasonFor(followed.code)
        return HealthCheckResult(
            finalUrl = followed.finalUrl.toString(),
            status = statusOf(reason),
            httpStatus = followed.code,
            latencyMs = latencyMs,
            redirects = followed.redirects,
            reason = reason,
            checkedAt = checkedAt,
        )
    }

    fun fromFailure(failure: FollowFailedException, latencyMs: Long, checkedAt: Instant): HealthCheckResult {
        val reason = reasonFor(failure.cause)
        return HealthCheckResult(
            finalUrl = failure.lastUrl.toString(),
            status = statusOf(reason),
            httpStatus = NO_RESPONSE,
            latencyMs = latencyMs,
            redirects = failure.redirects,
            reason = reason,
            checkedAt = checkedAt,
        )
    }

    fun toMessage(reason: HealthReason): String = when (reason) {
        HealthReason.SUCCESS -> "The site is up and responding"
        HealthReason.TIMEOUT -> "The site took too long to respond"
        HealthReason.DNS_ERROR -> "The domain could not be resolved"
        HealthReason.CONNECTION_ERROR -> "Could not connect to the site"
        HealthReason.TLS_ERROR -> "The site's TLS certificate is invalid or expired"
        HealthReason.AUTH_REQUIRED -> "The site requires authentication"
        HealthReason.ACCESS_DENIED -> "The site denied access to the checker"
        HealthReason.HTTP_4XX -> "The site answered with a client error (4xx)"
        HealthReason.HTTP_5XX -> "The site answered with a server error (5xx)"
        HealthReason.TOO_MANY_REDIRECTS -> "The site redirected too many times"
        HealthReason.PRIVATE_ADDRESS -> "Private or internal addresses are not allowed"
        HealthReason.PORT_NOT_ALLOWED -> "Only ports 80 and 443 are allowed"
    }

    private fun reasonFor(httpCode: Int): HealthReason = when (httpCode) {
        in 200..399 -> HealthReason.SUCCESS
        401 -> HealthReason.AUTH_REQUIRED
        403 -> HealthReason.ACCESS_DENIED
        in 400..499 -> HealthReason.HTTP_4XX
        else -> HealthReason.HTTP_5XX
    }

    private fun reasonFor(error: IOException): HealthReason = when (error) {
        is RejectedAddressException -> error.reason
        is TooManyRedirectsException -> HealthReason.TOO_MANY_REDIRECTS
        is InterruptedIOException -> HealthReason.TIMEOUT
        is UnknownHostException -> HealthReason.DNS_ERROR
        is SSLException -> HealthReason.TLS_ERROR
        else -> HealthReason.CONNECTION_ERROR
    }

    private fun statusOf(reason: HealthReason): HealthStatus = when (reason) {
        HealthReason.SUCCESS -> HealthStatus.UP

        HealthReason.AUTH_REQUIRED, HealthReason.ACCESS_DENIED -> HealthStatus.BLOCKED

        HealthReason.PRIVATE_ADDRESS, HealthReason.PORT_NOT_ALLOWED -> HealthStatus.REJECTED

        HealthReason.TIMEOUT,
        HealthReason.DNS_ERROR,
        HealthReason.CONNECTION_ERROR,
        HealthReason.TLS_ERROR,
        HealthReason.HTTP_4XX,
        HealthReason.HTTP_5XX,
        HealthReason.TOO_MANY_REDIRECTS,
        -> HealthStatus.DOWN
    }
}
