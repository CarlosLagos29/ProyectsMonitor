import type { MeResponseDto, SessionResponseDto } from '../dto/auth.dto'
import type { Me, Session } from '../api.types'

export function toSession(dto: SessionResponseDto): Session {
  return {
    token: dto.token,
    expiresAt: new Date(dto.expiresAt),
    user: dto.user,
  }
}

export function toMe(dto: MeResponseDto): Me {
  return {
    user: dto.user,
    expiresAt: new Date(dto.expiresAt),
    limits: dto.limits,
  }
}
