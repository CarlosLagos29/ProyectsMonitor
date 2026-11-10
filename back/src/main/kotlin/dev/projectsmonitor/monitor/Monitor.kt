package dev.projectsmonitor.monitor

import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "monitors",
    indexes = [
        Index(name = "idx_monitors_next_check_at", columnList = "next_check_at"),
        Index(name = "idx_monitors_expires_at", columnList = "expires_at"),
        Index(name = "idx_monitors_is_public", columnList = "is_public"),
    ],
)
class Monitor(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", unique = true)
    val ownerId: UUID? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    @Column(nullable = false, length = 60)
    var name: String,

    @Column(nullable = false, columnDefinition = "text")
    var url: String,

    @Column(name = "interval_seconds", nullable = false)
    var intervalSeconds: Int,

    @Column(name = "timeout_seconds")
    var timeoutSeconds: Int? = null,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "next_check_at", nullable = false)
    var nextCheckAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 16)
    var lastStatus: HealthStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "last_reason", length = 32)
    var lastReason: HealthReason? = null,

    @Column(name = "last_http_status")
    var lastHttpStatus: Int? = null,

    @Column(name = "last_latency_ms")
    var lastLatencyMs: Int? = null,

    @Column(name = "last_checked_at")
    var lastCheckedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
