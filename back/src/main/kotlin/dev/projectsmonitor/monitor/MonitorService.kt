package dev.projectsmonitor.monitor

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.check.UrlNormalizer
import dev.projectsmonitor.common.enums.HealthStatus
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.MonitorListQuery
import dev.projectsmonitor.monitor.dto.MonitorScope
import dev.projectsmonitor.monitor.dto.UpdateMonitorRequest
import dev.projectsmonitor.monitor.mapper.MonitorMapper
import dev.projectsmonitor.ratelimit.KeyedRateLimiter
import dev.projectsmonitor.ratelimit.RateLimit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.roundToLong

data class Uptime(val checks: Long, val upChecks: Long) {
    val percentage: Double? get() = if (checks == 0L) null else (upChecks * 1000.0 / checks).roundToLong() / 10.0
}

data class MonitorView(
    val monitor: Monitor,
    val owned: Boolean,
    val uptime: Uptime,
    val recentChecks: List<RecentCheck>,
)

data class MonitorStats(
    val from: Instant,
    val to: Instant,
    val uptime: Uptime,
    val byStatus: Map<HealthStatus, Long>,
    val latency: LatencyStats,
)

data class MonitorResults(val from: Instant, val to: Instant, val results: List<CheckResult>)

fun monitorNotFound() = ApiException(HttpStatus.NOT_FOUND, ApiErrorReason.NOT_FOUND, "Monitor not found")

@Service
class MonitorService(
    private val monitorRepository: MonitorRepository,
    private val checkResultRepository: CheckResultRepository,
    private val monitorChecker: MonitorChecker,
    properties: AppProperties,
) {

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(MonitorService::class.java)
        val UPTIME_WINDOW: Duration = Duration.ofHours(24)
        const val RECENT_CHECKS = 24
    }

    private val limits = properties.limits
    private val manualCheckCooldown = KeyedRateLimiter("manual-check", RateLimit(1, limits.manualCheckCooldown))
    private val guestManualChecks = KeyedRateLimiter("guest-manual-checks", limits.guest.manualChecks)

    @Transactional(readOnly = true)
    fun list(user: CurrentUser, query: MonitorListQuery): Page<MonitorView> {
        val now = now()
        val monitors = findPage(user, query.scope, now, PageRequest.of(query.page, query.size))
        return PageImpl(viewsOf(monitors.content, user, now), monitors.pageable, monitors.totalElements)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID, user: CurrentUser): MonitorView {
        val now = now()
        return viewOf(findVisible(id, user, now), user, now)
    }

    @Transactional(readOnly = true)
    fun stats(id: UUID, user: CurrentUser): MonitorStats {
        val now = now()
        val from = now.minus(UPTIME_WINDOW)
        val monitor = findVisible(id, user, now)
        val counts = checkResultRepository.countByStatus(monitor.id, from).associate { it.status to it.checks }
        val byStatus = HealthStatus.entries.associateWith { counts[it] ?: 0L }
        val uptime = Uptime(byStatus.values.sum(), byStatus.getValue(HealthStatus.UP))
        return MonitorStats(from, now, uptime, byStatus, checkResultRepository.findUpLatency(monitor.id, from))
    }

    @Transactional(readOnly = true)
    fun results(id: UUID, user: CurrentUser): MonitorResults {
        val now = now()
        val from = now.minus(UPTIME_WINDOW)
        val monitor = findVisible(id, user, now)
        return MonitorResults(from, now, checkResultRepository.findSince(monitor.id, from))
    }

    @Transactional
    fun create(request: CreateMonitorRequest, user: CurrentUser): MonitorView {
        val now = now()
        rejectPublic(request.isPublic, null, user)
        val url = UrlNormalizer.normalize(request.url)
        user.guestId?.let { reserveGuestSlot(it, now) }
        val monitor = try {
            monitorRepository.saveAndFlush(MonitorMapper.toNew(request, url, user, now))
        } catch (e: DataIntegrityViolationException) {
            throw monitorLimitReached()
        }
        LOG.info("Monitor created: id={} owner={} url={}", monitor.id, monitor.ownerId ?: CurrentUser.ADMIN_ID, url)
        return MonitorView(monitor, owned = true, uptime = Uptime(0, 0), recentChecks = emptyList())
    }

    @Transactional
    fun update(id: UUID, request: UpdateMonitorRequest, user: CurrentUser): MonitorView {
        val now = now()
        val current = findManageable(id, user, now)
        rejectPublic(request.isPublic, current, user)
        val updated = monitorRepository.save(MonitorMapper.toUpdated(current, request, now))
        LOG.info("Monitor updated: id={} enabled={} isPublic={}", updated.id, updated.enabled, updated.isPublic)
        return viewOf(updated, user, now)
    }

    @Transactional
    fun delete(id: UUID, user: CurrentUser) {
        val monitor = findManageable(id, user, now())
        monitorRepository.delete(monitor)
        LOG.info("Monitor deleted: id={}", monitor.id)
    }

    fun check(id: UUID, user: CurrentUser): CheckResult {
        val monitor = findManageable(id, user, now())
        manualCheckCooldown.acquire(monitor.id.toString())
        user.guestId?.let { guestManualChecks.acquire(it.toString()) }
        return monitorChecker.check(monitor)
    }

    private fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    private fun reserveGuestSlot(guestId: UUID, now: Instant) {
        monitorRepository.findByOwnerId(guestId)?.let { existing ->
            if (existing.expiresAt?.isAfter(now) == true) throw monitorLimitReached()
            monitorRepository.delete(existing)
            monitorRepository.flush()
        }
        if (monitorRepository.countActiveGuestMonitors(now) >= limits.guest.maxActive) {
            val retryAfter = monitorRepository.findOldestGuestExpiry(now)?.let { Duration.between(now, it) }
            LOG.warn("Guest capacity reached: max={}", limits.guest.maxActive)
            throw ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorReason.GUEST_CAPACITY_REACHED,
                "The demo is full right now. Try again later.",
                retryAfter,
            )
        }
    }

    private fun findPage(user: CurrentUser, scope: MonitorScope, now: Instant, pageable: Pageable): Page<Monitor> =
        when (scope) {
            MonitorScope.OWN ->
                user.guestId
                    ?.let { monitorRepository.findActiveGuestMonitor(it, now, pageable) }
                    ?: monitorRepository.findAdminMonitors(pageable)

            MonitorScope.PUBLIC -> monitorRepository.findPublicMonitors(pageable)

            MonitorScope.GUESTS -> if (user.isAdmin) {
                monitorRepository.findGuestMonitors(now, pageable)
            } else {
                throw forbidden("Only the admin can list guest monitors")
            }
        }

    private fun findVisible(id: UUID, user: CurrentUser, now: Instant): Monitor {
        val monitor = monitorRepository.findById(id).orElseThrow { monitorNotFound() }
        val visible = monitor.isOwnedBy(user, now) ||
            (user.isAdmin && monitor.isActiveGuestMonitor(now)) ||
            (!user.isAdmin && monitor.ownerId == null && monitor.isPublic)
        if (!visible) throw monitorNotFound()
        return monitor
    }

    private fun findManageable(id: UUID, user: CurrentUser, now: Instant): Monitor {
        val monitor = findVisible(id, user, now)
        if (!user.isAdmin && !monitor.isOwnedBy(user, now)) throw forbidden("This monitor is read-only")
        return monitor
    }

    private fun Monitor.isActiveGuestMonitor(now: Instant): Boolean = ownerId != null && expiresAt?.isAfter(now) == true

    private fun Monitor.isOwnedBy(user: CurrentUser, now: Instant): Boolean =
        if (user.isAdmin) ownerId == null else ownerId == user.guestId && expiresAt?.isAfter(now) == true

    private fun rejectPublic(isPublic: Boolean?, monitor: Monitor?, user: CurrentUser) {
        if (isPublic != true) return
        if (!user.isAdmin) throw forbidden("Guests cannot make monitors public")
        if (monitor?.ownerId != null) throw forbidden("Guest monitors cannot be public")
    }

    private fun forbidden(detail: String) =
        ApiException(HttpStatus.FORBIDDEN, ApiErrorReason.FORBIDDEN_FOR_ROLE, detail)

    private fun viewOf(monitor: Monitor, user: CurrentUser, now: Instant) = viewsOf(listOf(monitor), user, now).single()

    private fun viewsOf(monitors: List<Monitor>, user: CurrentUser, now: Instant): List<MonitorView> {
        if (monitors.isEmpty()) return emptyList()
        val ids = monitors.map { it.id }
        val from = now.minus(UPTIME_WINDOW)
        val uptimes = checkResultRepository.countUptime(ids, from).associate {
            it.monitorId to
                Uptime(it.checks, it.upChecks)
        }
        val recent = checkResultRepository.findRecent(ids, from, RECENT_CHECKS).groupBy { it.monitorId }
        return monitors.map {
            MonitorView(it, it.isOwnedBy(user, now), uptimes[it.id] ?: Uptime(0, 0), recent[it.id].orEmpty())
        }
    }

    private fun monitorLimitReached() =
        ApiException(HttpStatus.CONFLICT, ApiErrorReason.MONITOR_LIMIT_REACHED, "Guests can have only one monitor")
}
