package dev.projectsmonitor.monitor

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.check.UrlNormalizer
import dev.projectsmonitor.common.nowMillis
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.CheckResultsResponse
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.MonitorListQuery
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import dev.projectsmonitor.monitor.dto.MonitorScope
import dev.projectsmonitor.monitor.dto.MonitorStatsResponse
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
import java.util.UUID

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
        const val RECENT_CHECKS = 24
    }

    private val limits = properties.limits
    private val retention = properties.retention
    private val manualCheckCooldown = KeyedRateLimiter("manual-check", RateLimit(1, limits.manualCheckCooldown))
    private val guestManualChecks = KeyedRateLimiter("guest-manual-checks", limits.guest.manualChecks)

    @Transactional(readOnly = true)
    fun list(user: CurrentUser, query: MonitorListQuery): MonitorListResponse {
        val now = nowMillis()
        val monitors = findPage(user, query.scope, now, PageRequest.of(query.page, query.size))
        val views = viewsOf(monitors.content, user, now)
        return MonitorMapper.toList(PageImpl(views, monitors.pageable, monitors.totalElements))
    }

    @Transactional(readOnly = true)
    fun get(id: UUID, user: CurrentUser): MonitorResponse {
        val now = nowMillis()
        return viewOf(findVisible(id, user, now), user, now)
    }

    @Transactional(readOnly = true)
    fun stats(id: UUID, user: CurrentUser): MonitorStatsResponse {
        val now = nowMillis()
        val from = now.minus(retention)
        val monitor = findVisible(id, user, now)
        val counts = checkResultRepository.countByStatus(monitor.id, from)
        return MonitorMapper.toStats(from, now, counts, checkResultRepository.findUpLatency(monitor.id, from))
    }

    @Transactional(readOnly = true)
    fun results(id: UUID, user: CurrentUser): CheckResultsResponse {
        val now = nowMillis()
        val from = now.minus(retention)
        val monitor = findVisible(id, user, now)
        return MonitorMapper.toResults(from, now, checkResultRepository.findSince(monitor.id, from))
    }

    @Transactional
    fun create(request: CreateMonitorRequest, user: CurrentUser): MonitorResponse {
        val now = nowMillis()
        rejectPublic(request.isPublic, null, user)
        val url = UrlNormalizer.normalize(request.url)
        user.guestId?.let { reserveGuestSlot(it, now) }
        val monitor = try {
            monitorRepository.saveAndFlush(MonitorMapper.toNew(request, url, user, now))
        } catch (e: DataIntegrityViolationException) {
            throw monitorLimitReached()
        }
        LOG.info("Monitor created: id={} owner={} url={}", monitor.id, monitor.ownerId ?: CurrentUser.ADMIN_ID, url)
        return MonitorMapper.toView(monitor, owned = true, uptime = null, recentChecks = emptyList())
    }

    @Transactional
    fun update(id: UUID, request: UpdateMonitorRequest, user: CurrentUser): MonitorResponse {
        val now = nowMillis()
        val current = findManageable(id, user, now)
        rejectPublic(request.isPublic, current, user)
        val updated = MonitorMapper.toUpdated(current, request, now)
        LOG.info("Monitor updated: id={} enabled={} isPublic={}", updated.id, updated.enabled, updated.isPublic)
        return viewOf(updated, user, now)
    }

    @Transactional
    fun delete(id: UUID, user: CurrentUser) {
        val monitor = findManageable(id, user, nowMillis())
        monitorRepository.delete(monitor)
        LOG.info("Monitor deleted: id={}", monitor.id)
    }

    fun check(id: UUID, user: CurrentUser): CheckResultResponse {
        val monitor = findManageable(id, user, nowMillis())
        manualCheckCooldown.acquire(monitor.id.toString())
        user.guestId?.let { guestManualChecks.acquire(it.toString()) }
        return MonitorMapper.toCheckResult(monitorChecker.check(monitor))
    }

    private fun reserveGuestSlot(guestId: UUID, now: Instant) {
        if (monitorRepository.existsByOwnerId(guestId)) throw monitorLimitReached()
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
            (user.isAdmin && monitor.isActiveGuest(now)) ||
            (!user.isAdmin && monitor.ownerId == null && monitor.isPublic)
        if (!visible) throw monitorNotFound()
        return monitor
    }

    private fun findManageable(id: UUID, user: CurrentUser, now: Instant): Monitor {
        val monitor = findVisible(id, user, now)
        if (!user.isAdmin && !monitor.isOwnedBy(user, now)) throw forbidden("This monitor is read-only")
        return monitor
    }

    private fun Monitor.isActiveGuest(now: Instant): Boolean = ownerId != null && expiresAt?.isAfter(now) == true

    private fun Monitor.isOwnedBy(user: CurrentUser, now: Instant): Boolean =
        if (user.isAdmin) ownerId == null else ownerId == user.guestId && isActiveGuest(now)

    private fun rejectPublic(isPublic: Boolean?, monitor: Monitor?, user: CurrentUser) {
        if (isPublic != true) return
        if (!user.isAdmin) throw forbidden("Guests cannot make monitors public")
        if (monitor?.ownerId != null) throw forbidden("Guest monitors cannot be public")
    }

    private fun forbidden(detail: String) =
        ApiException(HttpStatus.FORBIDDEN, ApiErrorReason.FORBIDDEN_FOR_ROLE, detail)

    private fun viewOf(monitor: Monitor, user: CurrentUser, now: Instant) = viewsOf(listOf(monitor), user, now).single()

    private fun viewsOf(monitors: List<Monitor>, user: CurrentUser, now: Instant): List<MonitorResponse> {
        if (monitors.isEmpty()) return emptyList()
        val ids = monitors.map { it.id }
        val from = now.minus(retention)
        val uptimes = checkResultRepository.countUptime(ids, from).associateBy { it.monitorId }
        val recent = checkResultRepository.findRecent(ids, from, RECENT_CHECKS).groupBy { it.monitorId }
        return monitors.map {
            MonitorMapper.toView(it, it.isOwnedBy(user, now), uptimes[it.id], recent[it.id].orEmpty())
        }
    }

    private fun monitorLimitReached() =
        ApiException(HttpStatus.CONFLICT, ApiErrorReason.MONITOR_LIMIT_REACHED, "Guests can have only one monitor")
}
