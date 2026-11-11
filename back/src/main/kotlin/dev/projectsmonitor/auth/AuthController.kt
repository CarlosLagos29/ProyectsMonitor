package dev.projectsmonitor.auth

import dev.projectsmonitor.auth.dto.LoginRequest
import dev.projectsmonitor.auth.dto.MeResponse
import dev.projectsmonitor.auth.dto.TokenResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AuthController.BASE_PATH)
@Tag(name = "Auth", description = "Guest and admin sessions")
class AuthController(private val authService: AuthService) {

    companion object {
        const val BASE_PATH = "/api/v1/auth"
        val API_TOKEN_PATHS = arrayOf("$BASE_PATH/guest", "$BASE_PATH/login")
        private val LOG: Logger = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostMapping("/login")
    @LoginDocs
    fun login(@Valid @RequestBody request: LoginRequest, httpRequest: HttpServletRequest): TokenResponse {
        LOG.info("Admin login requested")
        val issued = authService.login(request.email, request.password, httpRequest.remoteAddr)
        LOG.info("Admin token issued: expiresAt={}", issued.user.expiresAt)
        return TokenResponse.from(issued)
    }

    @PostMapping("/guest")
    @GuestTokenDocs
    fun guest(httpRequest: HttpServletRequest): TokenResponse {
        LOG.info("Guest token requested")
        val issued = authService.issueGuestToken(httpRequest.remoteAddr)
        LOG.info("Guest token issued: guest={} expiresAt={}", issued.user.id, issued.user.expiresAt)
        return TokenResponse.from(issued)
    }

    @GetMapping("/me")
    @MeDocs
    fun me(user: CurrentUser): MeResponse = MeResponse.from(user, authService.limits)
}
