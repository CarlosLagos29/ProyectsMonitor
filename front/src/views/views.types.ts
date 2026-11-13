import type { Monitor } from '@/api/api.types'
import type { ErrorMessage } from '@/api/errors'

export interface LoginData {
  email: string
  password: string
  submitted: boolean
  serverFieldErrors: Record<string, string>
  loggingIn: boolean
  loginError: ErrorMessage | null
  enteringAsGuest: boolean
  guestError: ErrorMessage | null
  guestWaitSeconds: number
  guestTimer: ReturnType<typeof setInterval> | undefined
}

export type HomeTab = 'mine' | 'guests'

export interface HomeData {
  activeTab: HomeTab
  formOpen: boolean
  editing: Monitor | null
  deleteOpen: boolean
  deleteTarget: Monitor | null
  pollTimer: ReturnType<typeof setInterval> | undefined
  firstCheckTimer: ReturnType<typeof setTimeout> | undefined
}

export interface MonitorDetailData {
  loading: boolean
  notFound: boolean
  error: ErrorMessage | null
  formOpen: boolean
  deleteOpen: boolean
}
