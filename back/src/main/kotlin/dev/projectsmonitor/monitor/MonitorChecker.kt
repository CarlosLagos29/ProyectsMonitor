package dev.projectsmonitor.monitor

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.projectsmonitor.check.HealthCheckResult
import dev.projectsmonitor.check.HealthCheckService
import dev.projectsmonitor.common.toLogSafe
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.config.HealthCheckProperties
import dev.projectsmonitor.monitor.mapper.MonitorMapper
import dev.projectsmonitor.ratelimit.KeyedRateLimiter
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

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
        const val MAX_CACHED_URLS = 10_000L
    }

    private val hostLimiter = KeyedRateLimiter("host", checkProperties.perHost)
    private val recentResults: Cache<String, HealthCheckResult> = Caffeine.newBuilder()
        .expireAfterWrite(checkProperties.resultTtl)
        .maximumSize(MAX_CACHED_URLS)
        .build()

    fun check(monitor: Monitor): CheckResult {
        val result = requireNotNull(resultFor(monitor.url, wait = true))
        return transactionTemplate.execute { record(monitor.id, result) } ?: throw monitorNotFound()
    }

    fun checkScheduled(monitors: List<Monitor>): Boolean {
        val result = resultFor(monitors.first().url, wait = false) ?: return false
        transactionTemplate.executeWithoutResult { monitors.forEach { record(it.id, result) } }
        return true
    }

    private fun resultFor(url: String, wait: Boolean): HealthCheckResult? {
        recentResults.getIfPresent(url)?.let {
            LOG.debug("Check result reused: url={} checkedAt={}", url.toLogSafe(), it.checkedAt)
            return it
        }
        val target = url.toHttpUrl()
        if (wait) {
            hostLimiter.acquire(target.host)
        } else if (!hostLimiter.tryAcquire(target.host)) {
            return null
        }
        return healthCheckService.check(target).also { recentResults.put(url, it) }
    }

    private fun record(monitorId: UUID, result: HealthCheckResult): CheckResult? {
        val monitor = monitorRepository.findById(monitorId).orElse(null) ?: return null
        val checkedAt = result.checkedAt
        if (monitor.lastCheckedAt?.isBefore(checkedAt) == false) {
            return checkResultRepository.findFirstByMonitorIdOrderByCheckedAtDesc(monitor.id)
        }
        val previousStatus = monitor.lastStatus
        monitor.lastStatus = result.status
        monitor.lastReason = result.reason
        monitor.lastHttpStatus = result.httpStatus
        monitor.lastLatencyMs = result.latencyMs.toInt()
        monitor.lastCheckedAt = checkedAt
        monitor.nextCheckAt = checkedAt.plus(appProperties.limits.checkInterval)
        LOG.info("Monitor checked: id={} status={} reason={}", monitor.id, result.status, result.reason)
        if (previousStatus != null && previousStatus != result.status) {
            LOG.warn("Monitor status changed: id={} from={} to={}", monitor.id, previousStatus, result.status)
        }
        return checkResultRepository.save(MonitorMapper.toNewResult(monitor, result, checkedAt))
    }
}
