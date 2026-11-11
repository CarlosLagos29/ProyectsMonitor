package dev.projectsmonitor.monitor

import dev.projectsmonitor.check.HealthCheckResult
import dev.projectsmonitor.check.HealthCheckService
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.config.HealthCheckProperties
import dev.projectsmonitor.ratelimit.KeyedRateLimiter
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.temporal.ChronoUnit

@Service
class MonitorChecker(
    private val healthCheckService: HealthCheckService,
    private val monitorRepository: MonitorRepository,
    private val checkResultRepository: CheckResultRepository,
    private val transactionTemplate: TransactionTemplate,
    private val appProperties: AppProperties,
    checkProperties: HealthCheckProperties,
) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(MonitorChecker::class.java)
    }

    private val hostLimiter = KeyedRateLimiter("host", checkProperties.perHost)

    fun check(monitor: Monitor): CheckResult {
        val url = monitor.url.toHttpUrl()
        hostLimiter.acquire(url.host)
        val result = healthCheckService.check(url)
        LOG.info("Monitor checked: id={} status={} reason={}", monitor.id, result.status, result.reason)
        return requireNotNull(transactionTemplate.execute { record(monitor, result) })
    }

    private fun record(checked: Monitor, result: HealthCheckResult): CheckResult {
        val monitor = monitorRepository.findById(checked.id).orElseThrow { monitorNotFound() }
        val checkedAt = result.checkedAt.truncatedTo(ChronoUnit.MILLIS)
        monitor.lastStatus = result.status
        monitor.lastReason = result.reason
        monitor.lastHttpStatus = result.httpStatus
        monitor.lastLatencyMs = result.latencyMs.toInt()
        monitor.lastCheckedAt = checkedAt
        monitor.nextCheckAt = checkedAt.plus(appProperties.limits.checkInterval)
        return checkResultRepository.save(
            CheckResult(
                monitor = monitor,
                checkedAt = checkedAt,
                status = result.status,
                reason = result.reason,
                httpStatus = result.httpStatus,
                latencyMs = result.latencyMs.toInt(),
                finalUrl = result.finalUrl,
                redirects = result.redirects,
            ),
        )
    }
}
