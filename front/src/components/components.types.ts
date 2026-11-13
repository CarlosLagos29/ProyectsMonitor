import type { ErrorMessage } from '@/api/errors'

export interface MonitorCardData {
  now: number
  ticker: ReturnType<typeof setInterval> | undefined
  checking: boolean
  toggling: boolean
  cooldownUntil: number
  actionError: ErrorMessage | null
}

export interface MonitorFormData {
  name: string
  url: string
  isPublic: boolean
  enabled: boolean
  submitted: boolean
  saving: boolean
  fieldErrors: Record<string, string>
  error: ErrorMessage | null
}

export interface DeleteMonitorData {
  deleting: boolean
  error: ErrorMessage | null
}
