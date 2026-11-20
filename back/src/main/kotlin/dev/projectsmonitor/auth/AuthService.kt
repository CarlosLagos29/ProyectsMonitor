package dev.projectsmonitor.auth

import dev.projectsmonitor.auth.dto.MeResponse
import dev.projectsmonitor.auth.dto.TokenResponse
import dev.projectsmonitor.auth.mapper.AuthMapper
import dev.projectsmonitor.auth.token.AdminSession
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

    fun login(email: String, password: String, clientIp: String): TokenResponse {
        if (!credentialsMatch(email, password)) {
            LOG.warn("Failed admin login: ip={}", clientIp.toLogSafe())
            throw ApiException(HttpStatus.UNAUTHORIZED, ApiErrorReason.INVALID_CREDENTIALS, "Invalid email or password")
        }
        val issued = jwtService.issue(CurrentUser.ADMIN_ID, Role.ADMIN, adminSession.start())
        LOG.info("Admin logged in: ip={} expiresAt={}", clientIp.toLogSafe(), issued.user.expiresAt)
        return AuthMapper.toToken(issued)
    }

    fun issueGuestToken(clientIp: String): TokenResponse {
        guestTokenLimiter.acquire(clientIp)
        val issued = jwtService.issue(UUID.randomUUID().toString(), Role.GUEST)
        LOG.info("Guest token issued: guest={} expiresAt={}", issued.user.id, issued.user.expiresAt)
        return AuthMapper.toToken(issued)
    }

    fun me(user: CurrentUser): MeResponse = AuthMapper.toMe(user, properties.limits)

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
