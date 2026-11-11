package dev.projectsmonitor.auth.token

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.auth.Role
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class AdminSession : OAuth2TokenValidator<Jwt> {

    companion object {
        const val SESSION_CLAIM = "sid"
        private val REPLACED = OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Admin session is no longer active", null)
    }

    private val current = AtomicReference<String?>()

    fun start(): String = UUID.randomUUID().toString().also { current.set(it) }

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val isAdmin = token.getClaimAsString(CurrentUser.ROLE_CLAIM) == Role.ADMIN.name
        val sessionId = token.getClaimAsString(SESSION_CLAIM)
        return if (!isAdmin || (sessionId != null && sessionId == current.get())) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(REPLACED)
        }
    }
}
