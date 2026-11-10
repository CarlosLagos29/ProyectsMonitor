package dev.projectsmonitor.check

import dev.projectsmonitor.common.enums.HealthReason
import dev.projectsmonitor.common.enums.HealthStatus
import dev.projectsmonitor.monitor.Monitor
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

@Entity
@Table(
    name = "check_results",
    indexes = [
        Index(name = "idx_check_results_monitor_checked_at", columnList = "monitor_id, checked_at desc"),
        Index(name = "idx_check_results_checked_at", columnList = "checked_at"),
    ],
)
class CheckResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false, foreignKey = ForeignKey(name = "fk_check_results_monitor"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    val monitor: Monitor,

    @Column(name = "checked_at", nullable = false)
    val checkedAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val status: HealthStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val reason: HealthReason,

    @Column(name = "http_status", nullable = false)
    val httpStatus: Int,

    @Column(name = "latency_ms", nullable = false)
    val latencyMs: Int,

    @Column(name = "final_url", nullable = false, columnDefinition = "text")
    val finalUrl: String,

    @Column(nullable = false)
    val redirects: Int,
)
