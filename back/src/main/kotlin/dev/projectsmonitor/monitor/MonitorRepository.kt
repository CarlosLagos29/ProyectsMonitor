package dev.projectsmonitor.monitor

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface MonitorRepository : JpaRepository<Monitor, UUID> {

    @Query(
        value = "select m from Monitor m where m.ownerId is null order by m.createdAt desc, m.id",
        countQuery = "select count(m) from Monitor m where m.ownerId is null",
    )
    fun findAdminMonitors(pageable: Pageable): Page<Monitor>

    @Query(
        value = """
        select m from Monitor m
        where (m.ownerId = :guestId and m.expiresAt > :now) or (m.ownerId is null and m.isPublic = true)
        order by case when m.ownerId = :guestId then 0 else 1 end, m.createdAt desc, m.id
        """,
        countQuery = """
        select count(m) from Monitor m
        where (m.ownerId = :guestId and m.expiresAt > :now) or (m.ownerId is null and m.isPublic = true)
        """,
    )
    fun findVisibleToGuest(guestId: UUID, now: Instant, pageable: Pageable): Page<Monitor>

    fun findByOwnerId(ownerId: UUID): Monitor?

    @Query("select count(m) from Monitor m where m.ownerId is not null and m.expiresAt > :now")
    fun countActiveGuestMonitors(now: Instant): Long

    @Query("select min(m.expiresAt) from Monitor m where m.ownerId is not null and m.expiresAt > :now")
    fun findOldestGuestExpiry(now: Instant): Instant?

    @NativeQuery(
        """
        select * from monitors
        where enabled and next_check_at <= :now and (expires_at is null or expires_at > :now)
        order by next_check_at
        limit :limit
        for update skip locked
        """,
    )
    fun findDueForUpdate(now: Instant, limit: Int): List<Monitor>

    @Modifying
    @Query("delete from Monitor m where m.expiresAt <= :now")
    fun deleteExpired(now: Instant): Int
}
