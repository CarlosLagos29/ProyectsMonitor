package dev.projectsmonitor.auth.token

import dev.projectsmonitor.auth.AuthController
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.writeProblem
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

class ApiTokenFilter(private val apiToken: String) : OncePerRequestFilter() {

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI !in AuthController.API_TOKEN_PATHS

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (isValid(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            chain.doFilter(request, response)
            return
        }
        response.writeProblem(HttpStatus.UNAUTHORIZED, ApiErrorReason.INVALID_API_TOKEN, "Missing or invalid API token")
    }

    private fun isValid(header: String?): Boolean {
        if (apiToken.isBlank() || header == null || !header.startsWith(BEARER_PREFIX)) return false
        return MessageDigest.isEqual(header.removePrefix(BEARER_PREFIX).toByteArray(), apiToken.toByteArray())
    }
}
