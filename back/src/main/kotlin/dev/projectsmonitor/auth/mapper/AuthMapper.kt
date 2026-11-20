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

    fun toToken(issued: IssuedToken) = TokenResponse(
        token = issued.token,
        expiresAt = issued.user.expiresAt,
        user = toUser(issued.user),
    )

    fun toMe(user: CurrentUser, limits: AppProperties.Limits) = MeResponse(
        user = toUser(user),
        expiresAt = user.expiresAt,
        limits = toLimits(user.role, limits),
    )

    private fun toUser(user: CurrentUser) = UserResponse(id = user.id, role = user.role)

    private fun toLimits(role: Role, limits: AppProperties.Limits) = LimitsResponse(
        maxMonitors = if (role == Role.GUEST) GUEST_MAX_MONITORS else null,
        canSetPublic = role == Role.ADMIN,
        checkIntervalSeconds = limits.checkInterval.seconds,
        manualCheckCooldownSeconds = limits.manualCheckCooldown.seconds,
    )
}
