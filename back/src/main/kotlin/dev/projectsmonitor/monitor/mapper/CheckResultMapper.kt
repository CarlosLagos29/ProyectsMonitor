package dev.projectsmonitor.monitor.mapper

import dev.projectsmonitor.check.toMessage
import dev.projectsmonitor.monitor.CheckResult
import dev.projectsmonitor.monitor.MonitorResults
import dev.projectsmonitor.monitor.MonitorStats
import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.CheckResultsResponse
import dev.projectsmonitor.monitor.dto.LatencyResponse
import dev.projectsmonitor.monitor.dto.MonitorStatsResponse
import kotlin.math.roundToInt

object CheckResultMapper {

    fun toResponse(result: CheckResult) = CheckResultResponse(
        checkedAt = result.checkedAt,
        status = result.status,
        reason = result.reason,
        message = result.reason.toMessage(),
        httpStatus = result.httpStatus,
        latencyMs = result.latencyMs,
        finalUrl = result.finalUrl,
        redirects = result.redirects,
    )

    fun toResultsResponse(results: MonitorResults) = CheckResultsResponse(
        from = results.from,
        to = results.to,
        results = results.results.map { toResponse(it) },
    )

    fun toStatsResponse(stats: MonitorStats) = MonitorStatsResponse(
        from = stats.from,
        to = stats.to,
        uptime = MonitorMapper.toUptime(stats.uptime),
        byStatus = stats.byStatus,
        latency = LatencyResponse(avgMs = stats.latency.avgMs?.roundToInt(), p95Ms = stats.latency.p95Ms),
    )
}
