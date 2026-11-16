import type { LastCheckDto, RecentCheckDto, UptimeDto } from './check.dto'

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
  recentChecks: RecentCheckDto[]
  createdAt: string
  updatedAt: string
}

export interface PageDto {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export type MonitorScopeDto = 'OWN' | 'PUBLIC' | 'GUESTS'

export interface MonitorListQueryDto {
  page?: number
  size?: number
  scope?: MonitorScopeDto
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
