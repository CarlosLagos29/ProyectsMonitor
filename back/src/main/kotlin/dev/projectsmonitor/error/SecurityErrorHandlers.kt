package dev.projectsmonitor.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper

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

private val PROBLEM_MAPPER = JsonMapper.builder()
    .addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)
    .build()

fun HttpServletResponse.writeProblem(status: HttpStatus, reason: ApiErrorReason, detail: String) {
    this.status = status.value()
    contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    characterEncoding = Charsets.UTF_8.name()
    PROBLEM_MAPPER.writeValue(writer, problem(status, reason, detail))
}
