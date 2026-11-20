package dev.projectsmonitor.scheduler

import dev.projectsmonitor.common.nowMillis
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.monitor.CheckResultRepository
import dev.projectsmonitor.monitor.MonitorRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StartupCleanup(
    private val monitorRepository: MonitorRepository,
    private val checkResultRepository: CheckResultRepository,
    properties: AppProperties,
) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(StartupCleanup::class.java)
    }

    private val retention = properties.retention

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun cleanUp() {
        val now = nowMillis()
        val monitors = monitorRepository.deleteExpired(now)
        val results = checkResultRepository.deleteOlderThan(now.minus(retention))
        LOG.info("Startup cleanup done: expiredMonitors={} oldResults={}", monitors, results)
    }
}
