package dev.projectsmonitor.monitor

import dev.projectsmonitor.common.enums.HealthStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

data class UptimeCount(val monitorId: UUID, val checks: Long, val upChecks: Long)

data class RecentCheck(val monitorId: UUID, val checkedAt: Instant, val status: HealthStatus, val latencyMs: Int)

interface CheckResultRepository : JpaRepository<CheckResult, Long> {

    fun findFirstByMonitorIdOrderByCheckedAtDesc(monitorId: UUID): CheckResult?

    @Query(
        """
        select r from CheckResult r
        where r.monitor.id = :monitorId and r.checkedAt >= :from
        order by r.checkedAt
        """,
    )
    fun findSince(monitorId: UUID, from: Instant): List<CheckResult>

    @Query(
        """
        select new dev.projectsmonitor.monitor.UptimeCount(
            r.monitor.id,
            count(r),
            sum(case when r.status = dev.projectsmonitor.common.enums.HealthStatus.UP then 1L else 0L end)
        )
        from CheckResult r
        where r.monitor.id in :monitorIds and r.checkedAt >= :from
        group by r.monitor.id
        """,
    )
    fun countUptime(monitorIds: Collection<UUID>, from: Instant): List<UptimeCount>

    @Query(
        """
        select new dev.projectsmonitor.monitor.RecentCheck(t.monitorId, t.checkedAt, t.status, t.latencyMs)
        from (
            select r.monitor.id as monitorId, r.checkedAt as checkedAt, r.status as status, r.latencyMs as latencyMs,
                row_number() over (partition by r.monitor.id order by r.checkedAt desc) as position
            from CheckResult r
            where r.monitor.id in :monitorIds and r.checkedAt >= :from
        ) t
        where t.position <= :perMonitor
        order by t.checkedAt
        """,
    )
    fun findRecent(monitorIds: Collection<UUID>, from: Instant, perMonitor: Int): List<RecentCheck>

    @Modifying
    @Query("delete from CheckResult r where r.checkedAt < :before")
    fun deleteOlderThan(before: Instant): Int
}
