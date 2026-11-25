import type { CheckSeries, Monitor } from '@/api/api.types'
import type { ErrorMessage } from '@/api/errors'

export interface LoginData {
  email: string
  password: string
  showPassword: boolean
  now: number
  clockTimer: ReturnType<typeof setInterval> | undefined
  submitted: boolean
  serverFieldErrors: Record<string, string>
  loggingIn: boolean
  loginError: ErrorMessage | null
  failedAttempts: number
  enteringAsGuest: boolean
  guestError: ErrorMessage | null
  guestWaitSeconds: number
  guestTimer: ReturnType<typeof setInterval> | undefined
}

export interface HomeData {
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
  series: CheckSeries | null
  seriesLoading: boolean
  seriesError: ErrorMessage | null
  pollTimer: ReturnType<typeof setInterval> | undefined
}
