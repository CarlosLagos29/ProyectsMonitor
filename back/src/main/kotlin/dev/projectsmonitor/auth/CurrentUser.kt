package dev.projectsmonitor.auth

import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.Instant
import java.util.UUID

enum class Role { ADMIN, GUEST }

data class CurrentUser(val id: String, val role: Role, val expiresAt: Instant) {

    val isAdmin: Boolean get() = role == Role.ADMIN

    val guestId: UUID? get() = if (role == Role.GUEST) UUID.fromString(id) else null

    companion object {
        const val ROLE_CLAIM = "role"
        const val ADMIN_ID = "admin"

        fun from(jwt: Jwt) = CurrentUser(
            id = requireNotNull(jwt.subject),
            role = Role.valueOf(requireNotNull(jwt.getClaimAsString(ROLE_CLAIM))),
            expiresAt = requireNotNull(jwt.expiresAt),
        )

        fun fromSecurityContext(): CurrentUser? =
            (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)?.let { from(it.token) }
    }
}

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == CurrentUser::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): CurrentUser = CurrentUser.fromSecurityContext()
        ?: throw AuthenticationCredentialsNotFoundException("No authenticated user")
}
