import type { LimitsDto, RoleDto, UserDto } from './dto/auth.dto'
import type { HealthReasonDto, HealthStatusDto, UptimeDto } from './dto/check.dto'

export type Role = RoleDto
export type User = UserDto
export type Limits = LimitsDto

export interface Session {
  token: string
  expiresAt: Date
  user: User
}

export interface Me {
  user: User
  expiresAt: Date
  limits: Limits
}

export type HealthStatus = HealthStatusDto
export type HealthReason = HealthReasonDto
export type Uptime = UptimeDto

export interface LastCheck {
  checkedAt: Date
  status: HealthStatus
  reason: HealthReason
  httpStatus: number
  latencyMs: number
}

export interface CheckResult extends LastCheck {
  finalUrl: string
  redirects: number
}

export interface Monitor {
  id: string
  name: string
  url: string
  isPublic: boolean
  enabled: boolean
  owned: boolean
  expiresAt: Date | null
  nextCheckAt: Date
  lastCheck: LastCheck | null
  uptime: Uptime
  createdAt: Date
  updatedAt: Date
}

export interface MonitorDraft {
  name: string
  url: string
  isPublic: boolean
}

export interface PageInfo {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface MonitorPage {
  monitors: Monitor[]
  page: PageInfo
}

export interface MonitorListParams {
  page?: number
  size?: number
}

export interface MonitorChanges {
  name?: string
  isPublic?: boolean
  enabled?: boolean
}

export const ERROR_REASONS = [
  'VALIDATION_ERROR',
  'INVALID_URL',
  'INVALID_API_TOKEN',
  'INVALID_CREDENTIALS',
  'SESSION_EXPIRED',
  'FORBIDDEN_FOR_ROLE',
  'NOT_FOUND',
  'MONITOR_LIMIT_REACHED',
  'RATE_LIMITED',
  'GUEST_CAPACITY_REACHED',
] as const

export type ErrorReason = (typeof ERROR_REASONS)[number]
