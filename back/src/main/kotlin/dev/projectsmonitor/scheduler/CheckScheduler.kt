package dev.projectsmonitor.scheduler

import dev.projectsmonitor.common.toLogSafe
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.monitor.Monitor
import dev.projectsmonitor.monitor.MonitorChecker
import dev.projectsmonitor.monitor.MonitorRepository
import jakarta.annotation.PreDestroy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class CheckScheduler(
    private val monitorRepository: MonitorRepository,
    private val monitorChecker: MonitorChecker,
    private val transactionTemplate: TransactionTemplate,
    properties: AppProperties,
) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(CheckScheduler::class.java)
        const val TICK_SECONDS = 60L
        const val INITIAL_DELAY_SECONDS = 10L
        const val BATCH_SIZE = 50
        const val POOL_SIZE = 8
    }

    private val checkInterval = properties.limits.checkInterval
    private val pool = Executors.newFixedThreadPool(POOL_SIZE, Thread.ofPlatform().name("check-", 0).factory())

    @Scheduled(fixedDelay = TICK_SECONDS, initialDelay = INITIAL_DELAY_SECONDS, timeUnit = TimeUnit.SECONDS)
    fun tick() {
        val due = claimDue()
        if (due.isEmpty()) return
        val groups = due.groupBy { it.url }.values
        val postponed = pool.invokeAll(groups.map { Callable { checkGroup(it) } }).flatMap { it.get() }
        if (postponed.isNotEmpty()) {
            transactionTemplate.executeWithoutResult { monitorRepository.reschedule(postponed.map { it.id }, now()) }
        }
        LOG.info("Tick done: due={} urls={} postponed={}", due.size, groups.size, postponed.size)
    }

    @PreDestroy
    fun shutdown() {
        pool.shutdownNow()
    }

    private fun claimDue(): List<Monitor> = transactionTemplate.execute {
        val now = now()
        monitorRepository.findDueForUpdate(now, BATCH_SIZE).onEach { it.nextCheckAt = now.plus(checkInterval) }
    }.orEmpty()

    private fun checkGroup(group: List<Monitor>): List<Monitor> = try {
        if (monitorChecker.checkScheduled(group)) emptyList() else group
    } catch (e: Exception) {
        LOG.error("Scheduled check failed: url={}", group.first().url.toLogSafe(), e)
        emptyList()
    }

    private fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
}
