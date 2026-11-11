package dev.projectsmonitor.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import dev.projectsmonitor.auth.AuthController
import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.auth.Role
import dev.projectsmonitor.auth.token.AdminSession
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import java.time.Duration
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig {

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
        val ROLE_NAMES = Role.entries.map { it.name }.toSet()
    }

    @Bean
    fun jwtSecretKey(properties: AppProperties): SecretKey =
        SecretKeySpec(properties.jwt.secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM)

    @Bean
    fun jwtEncoder(jwtSecretKey: SecretKey): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(jwtSecretKey))

    @Bean
    fun jwtDecoder(jwtSecretKey: SecretKey, adminSession: AdminSession): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .apply {
                setJwtValidator(
                    DelegatingOAuth2TokenValidator(
                        JwtTimestampValidator(Duration.ZERO),
                        JwtClaimValidator<String>(CurrentUser.ROLE_CLAIM) { it in ROLE_NAMES },
                        adminSession,
                    ),
                )
            }

    @Bean
    fun bearerTokenResolver(): BearerTokenResolver {
        val delegate = DefaultBearerTokenResolver()
        return BearerTokenResolver { request ->
            if (request.requestURI in AuthController.API_TOKEN_PATHS) null else delegate.resolve(request)
        }
    }
}
