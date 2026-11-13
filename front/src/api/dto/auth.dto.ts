export type RoleDto = 'ADMIN' | 'GUEST'

export interface UserDto {
  id: string
  role: RoleDto
}

export interface LimitsDto {
  maxMonitors: number | null
  canSetPublic: boolean
  checkIntervalSeconds: number
  manualCheckCooldownSeconds: number
}

export interface SessionResponseDto {
  token: string
  expiresAt: string
  user: UserDto
}

export interface MeResponseDto {
  user: UserDto
  expiresAt: string
  limits: LimitsDto
}

export interface LoginRequestDto {
  email: string
  password: string
}
