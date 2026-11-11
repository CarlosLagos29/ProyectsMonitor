package dev.projectsmonitor.config

import dev.projectsmonitor.ratelimit.RateLimit
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app")
class AppProperties(
    @NotBlank(message = "API_TOKEN is required")
    val apiToken: String,
    @Valid val jwt: Jwt,
    @Valid val admin: Admin,
    @Valid val cors: Cors,
    @Valid val limits: Limits,
    @Valid val authRateLimit: AuthRateLimit,
) {

    class Jwt(
        @NotBlank(message = "JWT_SECRET is required")
        @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
        val secret: String,
        val adminTtl: Duration,
        val guestTtl: Duration,
    )

    class Admin(
        @NotBlank(message = "ADMIN_EMAIL is required")
        val email: String,
        @NotBlank(message = "ADMIN_PASSWORD is required")
        val password: String,
    )

    class Cors(
        @NotEmpty(message = "CORS_ALLOWED_ORIGINS is required")
        val allowedOrigins: List<String>,
    )

    class Limits(val checkInterval: Duration, val manualCheckCooldown: Duration, @Valid val guest: GuestLimits)

    class GuestLimits(val maxActive: Int, val manualChecks: RateLimit, val requests: RateLimit)

    class AuthRateLimit(val guestToken: RateLimit)
}
