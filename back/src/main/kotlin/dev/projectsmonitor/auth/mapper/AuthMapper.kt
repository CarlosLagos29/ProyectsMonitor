package dev.projectsmonitor.auth.mapper

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.auth.Role
import dev.projectsmonitor.auth.dto.LimitsResponse
import dev.projectsmonitor.auth.dto.MeResponse
import dev.projectsmonitor.auth.dto.TokenResponse
import dev.projectsmonitor.auth.dto.UserResponse
import dev.projectsmonitor.auth.token.IssuedToken
import dev.projectsmonitor.config.AppProperties

object AuthMapper {

    private const val GUEST_MAX_MONITORS = 1

    fun toTokenResponse(issued: IssuedToken) = TokenResponse(
        token = issued.token,
        expiresAt = issued.user.expiresAt,
        user = toUserResponse(issued.user),
    )

    fun toMeResponse(user: CurrentUser, limits: AppProperties.Limits) = MeResponse(
        user = toUserResponse(user),
        expiresAt = user.expiresAt,
        limits = toLimitsResponse(user.role, limits),
    )

    private fun toUserResponse(user: CurrentUser) = UserResponse(id = user.id, role = user.role)

    private fun toLimitsResponse(role: Role, limits: AppProperties.Limits) = LimitsResponse(
        maxMonitors = if (role == Role.GUEST) GUEST_MAX_MONITORS else null,
        canSetPublic = role == Role.ADMIN,
        checkIntervalSeconds = limits.checkInterval.seconds,
        manualCheckCooldownSeconds = limits.manualCheckCooldown.seconds,
    )
}
