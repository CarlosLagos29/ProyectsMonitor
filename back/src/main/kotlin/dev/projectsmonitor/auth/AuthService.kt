package dev.projectsmonitor.auth

import dev.projectsmonitor.auth.token.AdminSession
import dev.projectsmonitor.auth.token.IssuedToken
import dev.projectsmonitor.auth.token.JwtService
import dev.projectsmonitor.common.toLogSafe
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import dev.projectsmonitor.ratelimit.KeyedRateLimiter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

@Service
class AuthService(
    private val properties: AppProperties,
    private val jwtService: JwtService,
    private val adminSession: AdminSession,
) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    private val guestTokenLimiter = KeyedRateLimiter("guest-token", properties.authRateLimit.guestToken)

    val limits: AppProperties.Limits get() = properties.limits

    fun login(email: String, password: String, clientIp: String): IssuedToken {
        if (!credentialsMatch(email, password)) {
            LOG.warn("Failed admin login: ip={}", clientIp.toLogSafe())
            throw ApiException(HttpStatus.UNAUTHORIZED, ApiErrorReason.INVALID_CREDENTIALS, "Invalid email or password")
        }
        LOG.warn("Admin logged in, previous admin session closed: ip={}", clientIp.toLogSafe())
        return jwtService.issue(CurrentUser.ADMIN_ID, Role.ADMIN, adminSession.start())
    }

    fun issueGuestToken(clientIp: String): IssuedToken {
        guestTokenLimiter.acquire(clientIp)
        return jwtService.issue(UUID.randomUUID().toString(), Role.GUEST)
    }

    private fun credentialsMatch(email: String, password: String): Boolean {
        val emailMatches = constantTimeEquals(email.trim().lowercase(), properties.admin.email.trim().lowercase())
        val passwordMatches = constantTimeEquals(sha256Hex(password), properties.admin.password.trim().lowercase())
        return emailMatches and passwordMatches
    }

    private fun sha256Hex(value: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)))

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
}
