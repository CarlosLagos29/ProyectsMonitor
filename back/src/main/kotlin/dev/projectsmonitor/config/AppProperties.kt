package dev.projectsmonitor.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app")
class AppProperties(
    @NotBlank(message = "API_TOKEN is required")
    val apiToken: String,
    @Valid val jwt: Jwt,
    @Valid val admin: Admin,
    @Valid val cors: Cors,
) {

    class Jwt(
        @NotBlank(message = "JWT_SECRET is required")
        val secret: String,
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
}
