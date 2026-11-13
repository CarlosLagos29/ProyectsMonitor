import type {
  CreateMonitorRequestDto,
  MonitorDto,
  UpdateMonitorRequestDto,
} from '../dto/monitor.dto'
import type { Monitor, MonitorChanges, MonitorDraft } from '../api.types'
import { toLastCheck } from './check.mapper'

export function toMonitor(dto: MonitorDto): Monitor {
  return {
    id: dto.id,
    name: dto.name,
    url: dto.url,
    isPublic: dto.isPublic,
    enabled: dto.enabled,
    owned: dto.owned,
    expiresAt: dto.expiresAt ? new Date(dto.expiresAt) : null,
    nextCheckAt: new Date(dto.nextCheckAt),
    lastCheck: dto.lastCheck ? toLastCheck(dto.lastCheck) : null,
    uptime: dto.uptime,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt),
  }
}

export function toCreateMonitorRequest(
  draft: MonitorDraft,
  canSetPublic: boolean,
): CreateMonitorRequestDto {
  const request: CreateMonitorRequestDto = { name: draft.name.trim(), url: draft.url.trim() }
  if (canSetPublic) {
    request.isPublic = draft.isPublic
  }
  return request
}

export function toUpdateMonitorRequest(
  changes: MonitorChanges,
  canSetPublic: boolean,
): UpdateMonitorRequestDto {
  const request: UpdateMonitorRequestDto = {}
  if (changes.name !== undefined) {
    request.name = changes.name.trim()
  }
  if (changes.enabled !== undefined) {
    request.enabled = changes.enabled
  }
  if (canSetPublic && changes.isPublic !== undefined) {
    request.isPublic = changes.isPublic
  }
  return request
}
