package dev.projectsmonitor.ratelimit

import dev.projectsmonitor.auth.CurrentUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor

class ApiRateLimitInterceptor(guestRequests: RateLimit) : HandlerInterceptor {

    private val guestLimiter = KeyedRateLimiter("api", guestRequests)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val user = CurrentUser.fromSecurityContext()
        if (user != null && !user.isAdmin) guestLimiter.acquire(user.id)
        return true
    }
}
