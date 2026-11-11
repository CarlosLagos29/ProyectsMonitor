package dev.projectsmonitor.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler

class SessionExpiredEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = response.writeProblem(
        HttpStatus.UNAUTHORIZED,
        ApiErrorReason.SESSION_EXPIRED,
        "Missing, invalid or expired session token",
    )
}

class ForbiddenForRoleHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = response.writeProblem(HttpStatus.FORBIDDEN, ApiErrorReason.FORBIDDEN_FOR_ROLE, "Not allowed for your role")
}

fun HttpServletResponse.writeProblem(status: HttpStatus, reason: ApiErrorReason, detail: String) {
    this.status = status.value()
    contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    writer.write(
        """{"title":"${status.reasonPhrase}","status":${status.value()},"detail":"$detail","reason":"$reason"}""",
    )
}
