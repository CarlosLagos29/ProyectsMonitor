package dev.projectsmonitor.monitor

import dev.projectsmonitor.auth.CurrentUser
import dev.projectsmonitor.monitor.dto.CheckResultResponse
import dev.projectsmonitor.monitor.dto.CheckResultsResponse
import dev.projectsmonitor.monitor.dto.CreateMonitorRequest
import dev.projectsmonitor.monitor.dto.MonitorListQuery
import dev.projectsmonitor.monitor.dto.MonitorListResponse
import dev.projectsmonitor.monitor.dto.MonitorResponse
import dev.projectsmonitor.monitor.dto.MonitorStatsResponse
import dev.projectsmonitor.monitor.dto.UpdateMonitorRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping(MonitorController.BASE_PATH)
@Tag(name = "Monitors", description = "Monitors visible to the current user and their manual checks")
class MonitorController(private val monitorService: MonitorService) {

    companion object {
        const val BASE_PATH = "/api/v1/monitors"
        private val LOG: Logger = LoggerFactory.getLogger(MonitorController::class.java)
    }

    @GetMapping
    @ListMonitorsDocs
    fun list(@Valid @ParameterObject query: MonitorListQuery, user: CurrentUser): MonitorListResponse {
        LOG.info("List monitors: user={} scope={} page={} size={}", user.id, query.scope, query.page, query.size)
        return monitorService.list(user, query)
    }

    @GetMapping("/{id}")
    @GetMonitorDocs
    fun get(@PathVariable id: UUID, user: CurrentUser): MonitorResponse {
        LOG.info("Get monitor: id={} user={}", id, user.id)
        return monitorService.get(id, user)
    }

    @PostMapping
    @CreateMonitorDocs
    fun create(@Valid @RequestBody request: CreateMonitorRequest, user: CurrentUser): ResponseEntity<MonitorResponse> {
        LOG.info("Create monitor: user={}", user.id)
        val response = monitorService.create(request, user)
        return ResponseEntity.created(URI.create("$BASE_PATH/${response.id}")).body(response)
    }

    @PatchMapping("/{id}")
    @UpdateMonitorDocs
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMonitorRequest,
        user: CurrentUser,
    ): MonitorResponse {
        LOG.info("Update monitor: id={} user={}", id, user.id)
        return monitorService.update(id, request, user)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMonitorDocs
    fun delete(@PathVariable id: UUID, user: CurrentUser) {
        LOG.info("Delete monitor: id={} user={}", id, user.id)
        monitorService.delete(id, user)
    }

    @GetMapping("/{id}/stats")
    @MonitorStatsDocs
    fun stats(@PathVariable id: UUID, user: CurrentUser): MonitorStatsResponse {
        LOG.info("Monitor stats: id={} user={}", id, user.id)
        return monitorService.stats(id, user)
    }

    @GetMapping("/{id}/results")
    @MonitorResultsDocs
    fun results(@PathVariable id: UUID, user: CurrentUser): CheckResultsResponse {
        LOG.info("Monitor results: id={} user={}", id, user.id)
        return monitorService.results(id, user)
    }

    @PostMapping("/{id}/check")
    @CheckMonitorDocs
    fun check(@PathVariable id: UUID, user: CurrentUser): CheckResultResponse {
        LOG.info("Manual check: id={} user={}", id, user.id)
        return monitorService.check(id, user)
    }
}
