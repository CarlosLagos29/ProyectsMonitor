package dev.projectsmonitor.config

import dev.projectsmonitor.ratelimit.HostRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimitConfig {

    @Bean
    fun hostRateLimiter(properties: HealthCheckProperties): HostRateLimiter = HostRateLimiter(properties.perHost)
}
