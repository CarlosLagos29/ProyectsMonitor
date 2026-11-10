package dev.projectsmonitor.check

import okhttp3.HttpUrl
import java.time.Instant

object HealthCheckMapper {

    fun fromResponse(url: HttpUrl, followed: FollowedResponse, latencyMs: Long, checkedAt: Instant): HealthCheckResult {
        val classification = HealthClassifier.classifyHttpCode(followed.code)
        return HealthCheckResult(
            url = url.toString(),
            finalUrl = followed.finalUrl.toString(),
            status = classification.status,
            httpStatus = followed.code,
            latencyMs = latencyMs,
            redirects = followed.redirects,
            reason = classification.reason,
            checkedAt = checkedAt,
        )
    }

    fun fromFailure(
        url: HttpUrl,
        failure: FollowFailedException,
        latencyMs: Long,
        checkedAt: Instant,
    ): HealthCheckResult {
        val classification = HealthClassifier.classifyError(failure.cause)
        return HealthCheckResult(
            url = url.toString(),
            finalUrl = failure.lastUrl.toString(),
            status = classification.status,
            latencyMs = latencyMs,
            redirects = failure.redirects,
            reason = classification.reason,
            checkedAt = checkedAt,
        )
    }
}
