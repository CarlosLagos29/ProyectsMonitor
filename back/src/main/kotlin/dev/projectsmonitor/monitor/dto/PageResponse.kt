package dev.projectsmonitor.monitor.dto

import io.swagger.v3.oas.annotations.media.Schema

data class PageResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Zero-based page number", example = "0")
    val number: Int,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Requested page size", example = "10")
    val size: Int,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Total monitors across all pages", example = "3")
    val totalElements: Long,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Total number of pages", example = "1")
    val totalPages: Int,
)
