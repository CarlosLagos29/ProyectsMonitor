package dev.projectsmonitor.check

import dev.projectsmonitor.common.enums.HealthStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

data class UptimeCount(val monitorId: UUID, val checks: Long, val upChecks: Long)

data class StatusCount(val status: HealthStatus, val checks: Long)

interface CheckResultRepository : JpaRepository<CheckResult, Long> {

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
        select new dev.projectsmonitor.check.UptimeCount(
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
        select new dev.projectsmonitor.check.StatusCount(r.status, count(r))
        from CheckResult r
        where r.monitor.id = :monitorId and r.checkedAt >= :from
        group by r.status
        """,
    )
    fun countByStatus(monitorId: UUID, from: Instant): List<StatusCount>

    @Modifying
    @Query("delete from CheckResult r where r.monitor.id = :monitorId")
    fun deleteByMonitorId(monitorId: UUID): Int

    @Modifying
    @Query("delete from CheckResult r where r.checkedAt < :before")
    fun deleteOlderThan(before: Instant): Int
}
