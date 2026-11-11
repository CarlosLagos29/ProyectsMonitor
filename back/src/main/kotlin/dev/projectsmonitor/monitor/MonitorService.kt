package dev.projectsmonitor.monitor

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.check.UrlNormalizer
import dev.projectsmonitor.config.AppProperties
import dev.projectsmonitor.error.ApiErrorReason
import dev.projectsmonitor.error.ApiException
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.UpdateMonitorRequest
import dev.projectsmonitor.monitor.mapper.MonitorMapper
import dev.projectsmonitor.ratelimit.KeyedRateLimiter
import dev.projectsmonitor.ratelimit.RateLimit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
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

data class MonitorView(val monitor: Monitor, val owned: Boolean, val uptime: Uptime)

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
    }

    private val limits = properties.limits
    private val manualCheckCooldown = KeyedRateLimiter("manual-check", RateLimit(1, limits.manualCheckCooldown))
    private val guestManualChecks = KeyedRateLimiter("guest-manual-checks", limits.guest.manualChecks)

    @Transactional(readOnly = true)
    fun list(user: CurrentUser): List<MonitorView> {
        val now = now()
        val monitors = user.guestId
            ?.let { monitorRepository.findVisibleToGuest(it, now) }
            ?: monitorRepository.findAllByOwnerIdIsNullOrderByNameAsc()
        val uptimes = uptimesOf(monitors.map { it.id }, now)
        return monitors
            .map { MonitorView(it, it.isOwnedBy(user, now), uptimes[it.id] ?: Uptime(0, 0)) }
            .sortedWith(compareByDescending<MonitorView> { it.owned }.thenBy { it.monitor.name.lowercase() })
    }

    @Transactional(readOnly = true)
    fun get(id: UUID, user: CurrentUser): MonitorView {
        val now = now()
        return viewOf(findVisible(id, user, now), user, now)
    }

    @Transactional
    fun create(request: CreateMonitorRequest, user: CurrentUser): MonitorView {
        val now = now()
        rejectPublicForGuest(request.isPublic, user)
        val url = UrlNormalizer.normalize(request.url)
        user.guestId?.let { reserveGuestSlot(it, now) }
        val monitor = try {
            monitorRepository.saveAndFlush(MonitorMapper.toNew(request, url, user, now))
        } catch (e: DataIntegrityViolationException) {
            throw monitorLimitReached()
        }
        LOG.info("Monitor created: id={} owner={} url={}", monitor.id, monitor.ownerId ?: CurrentUser.ADMIN_ID, url)
        return MonitorView(monitor, owned = true, uptime = Uptime(0, 0))
    }

    @Transactional
    fun update(id: UUID, request: UpdateMonitorRequest, user: CurrentUser): MonitorView {
        val now = now()
        val current = findOwned(id, user, now)
        rejectPublicForGuest(request.isPublic, user)
        val updated = monitorRepository.save(MonitorMapper.toUpdated(current, request, now))
        LOG.info("Monitor updated: id={} enabled={} isPublic={}", updated.id, updated.enabled, updated.isPublic)
        return viewOf(updated, user, now)
    }

    @Transactional
    fun delete(id: UUID, user: CurrentUser) {
        val monitor = findOwned(id, user, now())
        monitorRepository.delete(monitor)
        LOG.info("Monitor deleted: id={}", monitor.id)
    }

    fun check(id: UUID, user: CurrentUser): CheckResult {
        val monitor = findOwned(id, user, now())
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

    private fun findVisible(id: UUID, user: CurrentUser, now: Instant): Monitor {
        val monitor = monitorRepository.findById(id).orElseThrow { monitorNotFound() }
        val visible = monitor.isOwnedBy(user, now) || (!user.isAdmin && monitor.ownerId == null && monitor.isPublic)
        if (!visible) throw monitorNotFound()
        return monitor
    }

    private fun findOwned(id: UUID, user: CurrentUser, now: Instant): Monitor {
        val monitor = findVisible(id, user, now)
        if (!monitor.isOwnedBy(user, now)) {
            throw ApiException(HttpStatus.FORBIDDEN, ApiErrorReason.FORBIDDEN_FOR_ROLE, "This monitor is read-only")
        }
        return monitor
    }

    private fun Monitor.isOwnedBy(user: CurrentUser, now: Instant): Boolean =
        if (user.isAdmin) ownerId == null else ownerId == user.guestId && expiresAt?.isAfter(now) == true

    private fun rejectPublicForGuest(isPublic: Boolean?, user: CurrentUser) {
        if (isPublic == true && !user.isAdmin) {
            throw ApiException(
                HttpStatus.FORBIDDEN,
                ApiErrorReason.FORBIDDEN_FOR_ROLE,
                "Guests cannot make monitors public",
            )
        }
    }

    private fun viewOf(monitor: Monitor, user: CurrentUser, now: Instant) = MonitorView(
        monitor,
        monitor.isOwnedBy(user, now),
        uptimesOf(listOf(monitor.id), now)[monitor.id] ?: Uptime(0, 0),
    )

    private fun uptimesOf(ids: List<UUID>, now: Instant): Map<UUID, Uptime> {
        if (ids.isEmpty()) return emptyMap()
        return checkResultRepository.countUptime(ids, now.minus(UPTIME_WINDOW))
            .associate { it.monitorId to Uptime(it.checks, it.upChecks) }
    }

    private fun monitorLimitReached() =
        ApiException(HttpStatus.CONFLICT, ApiErrorReason.MONITOR_LIMIT_REACHED, "Guests can have only one monitor")
}
