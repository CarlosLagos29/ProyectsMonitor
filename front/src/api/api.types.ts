import type { LimitsDto, RoleDto, UserDto } from './dto/auth.dto'

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
