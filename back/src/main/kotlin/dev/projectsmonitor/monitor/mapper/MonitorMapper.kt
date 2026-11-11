package dev.projectsmonitor.monitor.mapper

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.check.toMessage
import dev.projectsmonitor.monitor.Monitor
import dev.projectsmonitor.monitor.MonitorView
import dev.projectsmonitor.monitor.Uptime
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.LastCheckResponse
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import dev.projectsmonitor.monitor.dto.UpdateMonitorRequest
import dev.projectsmonitor.monitor.dto.UptimeResponse
import okhttp3.HttpUrl
import java.time.Instant

object MonitorMapper {

    fun toNew(request: CreateMonitorRequest, url: HttpUrl, user: CurrentUser, now: Instant) = Monitor(
        ownerId = user.guestId,
        expiresAt = if (user.isAdmin) null else user.expiresAt,
        name = request.name.trim(),
        url = url.toString(),
        isPublic = request.isPublic ?: false,
        nextCheckAt = now,
        createdAt = now,
        updatedAt = now,
    )

    fun toUpdated(current: Monitor, request: UpdateMonitorRequest, now: Instant) = Monitor(
        id = current.id,
        ownerId = current.ownerId,
        expiresAt = current.expiresAt,
        name = request.name?.trim() ?: current.name,
        url = current.url,
        isPublic = request.isPublic ?: current.isPublic,
        enabled = request.enabled ?: current.enabled,
        nextCheckAt = current.nextCheckAt,
        lastStatus = current.lastStatus,
        lastReason = current.lastReason,
        lastHttpStatus = current.lastHttpStatus,
        lastLatencyMs = current.lastLatencyMs,
        lastCheckedAt = current.lastCheckedAt,
        createdAt = current.createdAt,
        updatedAt = now,
    )

    fun toResponse(view: MonitorView) = with(view.monitor) {
        MonitorResponse(
            id = id,
            name = name,
            url = url,
            isPublic = isPublic,
            enabled = enabled,
            owned = view.owned,
            expiresAt = expiresAt,
            nextCheckAt = nextCheckAt,
            lastCheck = toLastCheck(this),
            uptime = toUptime(view.uptime),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun toListResponse(views: List<MonitorView>) = MonitorListResponse(views.map { toResponse(it) })

    private fun toLastCheck(monitor: Monitor): LastCheckResponse? {
        val checkedAt = monitor.lastCheckedAt ?: return null
        val status = monitor.lastStatus ?: return null
        val reason = monitor.lastReason ?: return null
        return LastCheckResponse(
            checkedAt = checkedAt,
            status = status,
            reason = reason,
            message = reason.toMessage(),
            httpStatus = monitor.lastHttpStatus ?: 0,
            latencyMs = monitor.lastLatencyMs ?: 0,
        )
    }

    private fun toUptime(uptime: Uptime) = UptimeResponse(percentage = uptime.percentage, checks = uptime.checks)
}
