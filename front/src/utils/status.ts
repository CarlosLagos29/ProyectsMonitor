import { Ban, CircleCheck, CircleDashed, CircleX, ShieldAlert } from 'lucide-vue-next'

import type { HealthStatus } from '@/api/api.types'

export type StatusKey = HealthStatus | 'NONE'

export const STATUS_TEXT_CLASS: Record<StatusKey, string> = {
  UP: 'text-status-up border-status-up',
  DOWN: 'text-status-down border-status-down',
  BLOCKED: 'text-status-blocked border-status-blocked',
  REJECTED: 'text-status-rejected border-status-rejected',
  NONE: 'text-status-none border-status-none',
}

export const STATUS_ICON = {
  UP: CircleCheck,
  DOWN: CircleX,
  BLOCKED: ShieldAlert,
  REJECTED: Ban,
  NONE: CircleDashed,
} satisfies Record<StatusKey, unknown>

export function toStatusKey(status: HealthStatus | null | undefined): StatusKey {
  return status ?? 'NONE'
}
