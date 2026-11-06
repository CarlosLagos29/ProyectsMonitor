package dev.projectsmonitor.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

class ApiTokenFilter(private val apiToken: String) : OncePerRequestFilter() {

    companion object {
        val PROTECTED_PATHS = arrayOf("/api/v1/auth/guest", "/api/v1/auth/login")
        private const val BEARER_PREFIX = "Bearer "
        private const val UNAUTHORIZED_BODY =
            """{"title":"Unauthorized","status":401,"detail":"Missing or invalid API token","reason":"INVALID_API_TOKEN"}"""
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI !in PROTECTED_PATHS

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (isValid(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            chain.doFilter(request, response)
            return
        }

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write(UNAUTHORIZED_BODY)
    }

    private fun isValid(header: String?): Boolean {
        if (apiToken.isBlank() || header == null || !header.startsWith(BEARER_PREFIX)) return false
        return MessageDigest.isEqual(header.removePrefix(BEARER_PREFIX).toByteArray(), apiToken.toByteArray())
    }
}
