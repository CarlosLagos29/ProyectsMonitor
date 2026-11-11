package dev.projectsmonitor.ratelimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.projectsmonitor.common.toLogSafe
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import dev.projectsmonitor.error.toRetryAfterSeconds
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.time.Duration

data class RateLimit(val capacity: Long, val period: Duration)

class RateLimitExceededException(retryAfter: Duration) :
    ApiException(
        HttpStatus.TOO_MANY_REQUESTS,
        ApiErrorReason.RATE_LIMITED,
        "Rate limit exceeded. Try again in ${retryAfter.toRetryAfterSeconds()} seconds.",
        retryAfter,
    )

class KeyedRateLimiter(private val scope: String, private val limit: RateLimit) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(KeyedRateLimiter::class.java)
        const val MAX_TRACKED_KEYS = 10_000L
    }

    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(limit.period)
        .maximumSize(MAX_TRACKED_KEYS)
        .build()

    fun tryAcquire(key: String): Boolean = consume(key).isConsumed

    fun acquire(key: String) {
        val probe = consume(key)
        if (!probe.isConsumed) throw RateLimitExceededException(Duration.ofNanos(probe.nanosToWaitForRefill))
    }

    private fun consume(key: String): ConsumptionProbe {
        val probe = buckets.get(key) { newBucket() }.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            LOG.debug("Rate limit left: scope={} key={} left={}", scope, key.toLogSafe(), probe.remainingTokens)
        } else {
            LOG.warn("Rate limit hit: scope={} key={}", scope, key.toLogSafe())
        }
        return probe
    }

    private fun newBucket(): Bucket = Bucket.builder()
        .addLimit { it.capacity(limit.capacity).refillGreedy(limit.capacity, limit.period) }
        .build()
}
