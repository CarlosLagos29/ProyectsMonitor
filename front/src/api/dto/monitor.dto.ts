import type { LastCheckDto, UptimeDto } from './check.dto'

export interface MonitorDto {
  id: string
  name: string
  url: string
  isPublic: boolean
  enabled: boolean
  owned: boolean
  expiresAt: string | null
  nextCheckAt: string
  lastCheck: LastCheckDto | null
  uptime: UptimeDto
  createdAt: string
  updatedAt: string
}

export interface MonitorListResponseDto {
  monitors: MonitorDto[]
}

export interface CreateMonitorRequestDto {
  name: string
  url: string
  isPublic?: boolean
}

export interface UpdateMonitorRequestDto {
  name?: string
  isPublic?: boolean
  enabled?: boolean
}
