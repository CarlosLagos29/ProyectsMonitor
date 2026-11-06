package dev.projectsmonitor.ratelimit

import java.time.Duration

class RateLimitExceededException(val retryAfter: Duration) : RuntimeException("Rate limit exceeded")
