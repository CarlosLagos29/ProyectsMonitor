package dev.projectsmonitor.monitor.mapper

import dev.projectsmonitor.check.toMessage
import dev.projectsmonitor.monitor.CheckResult
import dev.projectsmonitor.monitor.dto.CheckResultResponse

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
}
