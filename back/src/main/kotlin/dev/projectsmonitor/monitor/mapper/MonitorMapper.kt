package dev.projectsmonitor.monitor.mapper

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.check.HealthCheckMapper
import dev.projectsmonitor.check.HealthCheckResult
import dev.projectsmonitor.common.enums.HealthStatus
import dev.projectsmonitor.monitor.CheckResult
import dev.projectsmonitor.monitor.LatencyStats
import dev.projectsmonitor.monitor.Monitor
import dev.projectsmonitor.monitor.RecentCheck
import dev.projectsmonitor.monitor.StatusCount
import dev.projectsmonitor.monitor.UptimeCount
import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.CheckResultsResponse
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.LastCheckResponse
import dev.projectsmonitor.monitor.dto.LatencyResponse
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import dev.projectsmonitor.monitor.dto.MonitorStatsResponse
import dev.projectsmonitor.monitor.dto.PageResponse
import dev.projectsmonitor.monitor.dto.RecentCheckResponse
import dev.projectsmonitor.monitor.dto.UpdateMonitorRequest
import dev.projectsmonitor.monitor.dto.UptimeResponse
import okhttp3.HttpUrl
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

    fun toUpdated(current: Monitor, request: UpdateMonitorRequest, now: Instant) = current.apply {
        request.name?.let { name = it.trim() }
        request.isPublic?.let { isPublic = it }
        request.enabled?.let { enabled = it }
        updatedAt = now
    }

    fun toChecked(current: Monitor, result: HealthCheckResult, checkInterval: Duration) = current.apply {
        lastStatus = result.status
        lastReason = result.reason
        lastHttpStatus = result.httpStatus
        lastLatencyMs = result.latencyMs
        lastCheckedAt = result.checkedAt
        nextCheckAt = result.checkedAt.plus(checkInterval)
    }

    fun toNewResult(monitor: Monitor, result: HealthCheckResult) = CheckResult(
        monitor = monitor,
        checkedAt = result.checkedAt,
        status = result.status,
        reason = result.reason,
        httpStatus = result.httpStatus,
        latencyMs = result.latencyMs,
        finalUrl = result.finalUrl,
        redirects = result.redirects,
    )

    fun toView(monitor: Monitor, owned: Boolean, uptime: UptimeCount?, recentChecks: List<RecentCheck>) =
        with(monitor) {
            MonitorResponse(
                id = id,
                name = name,
                url = url,
                isPublic = isPublic,
                enabled = enabled,
                owned = owned,
                expiresAt = expiresAt,
                nextCheckAt = nextCheckAt,
                lastCheck = toLastCheck(this),
                uptime = toUptime(uptime?.checks ?: 0, uptime?.upChecks ?: 0),
                recentChecks = recentChecks.map { toRecentCheck(it) },
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

    fun toList(page: Page<MonitorResponse>) = MonitorListResponse(
        monitors = page.content,
        page = PageResponse(
            number = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        ),
    )

    fun toStats(from: Instant, to: Instant, counts: List<StatusCount>, latency: LatencyStats): MonitorStatsResponse {
        val checksByStatus = counts.associate { it.status to it.checks }
        val byStatus = HealthStatus.entries.associateWith { checksByStatus[it] ?: 0L }
        return MonitorStatsResponse(
            from = from,
            to = to,
            uptime = toUptime(byStatus.values.sum(), byStatus.getValue(HealthStatus.UP)),
            byStatus = byStatus,
            latency = LatencyResponse(avgMs = latency.avgMs?.roundToInt(), p95Ms = latency.p95Ms),
        )
    }

    fun toResults(from: Instant, to: Instant, results: List<CheckResult>) = CheckResultsResponse(
        from = from,
        to = to,
        results = results.map { toCheckResult(it) },
    )

    fun toCheckResult(result: CheckResult) = CheckResultResponse(
        checkedAt = result.checkedAt,
        status = result.status,
        reason = result.reason,
        message = HealthCheckMapper.toMessage(result.reason),
        httpStatus = result.httpStatus,
        latencyMs = result.latencyMs,
        finalUrl = result.finalUrl,
        redirects = result.redirects,
    )

    private fun toUptime(checks: Long, upChecks: Long) = UptimeResponse(
        percentage = if (checks == 0L) null else (upChecks * 1000.0 / checks).roundToLong() / 10.0,
        checks = checks,
    )

    private fun toRecentCheck(check: RecentCheck) =
        RecentCheckResponse(checkedAt = check.checkedAt, status = check.status, latencyMs = check.latencyMs)

    private fun toLastCheck(monitor: Monitor): LastCheckResponse? {
        val checkedAt = monitor.lastCheckedAt ?: return null
        val status = monitor.lastStatus ?: return null
        val reason = monitor.lastReason ?: return null
        return LastCheckResponse(
            checkedAt = checkedAt,
            status = status,
            reason = reason,
            message = HealthCheckMapper.toMessage(reason),
            httpStatus = monitor.lastHttpStatus ?: 0,
            latencyMs = monitor.lastLatencyMs ?: 0,
        )
    }
}
