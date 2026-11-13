import type { ErrorMessage } from '@/api/errors'
import type { Limits, Monitor, User } from '@/api/api.types'

export type ExitReason = 'expiredGuest' | 'expiredAdmin'

export interface SessionState {
  token: string | null
  user: User | null
  expiresAt: Date | null
  limits: Limits | null
  exitReason: ExitReason | null
}

export interface StoredSession {
  token: string
  expiresAt: string
  user: User
}

export interface MonitorsState {
  items: Monitor[]
  loading: boolean
  loaded: boolean
  error: ErrorMessage | null
}
