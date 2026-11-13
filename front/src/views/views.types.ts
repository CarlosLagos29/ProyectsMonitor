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
