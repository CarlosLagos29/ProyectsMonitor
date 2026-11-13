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

export interface PageDto {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface MonitorListQueryDto {
  page?: number
  size?: number
}

export interface MonitorListResponseDto {
  monitors: MonitorDto[]
  page: PageDto
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
