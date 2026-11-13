import { isAxiosError } from 'axios'

import type { ProblemDetailDto } from './dto/problem.dto'
import { ERROR_REASONS, type ErrorReason } from './api.types'

interface ApiErrorInit {
  status: number
  reason: ErrorReason | null
  retryAfter: number | null
  fieldErrors: Record<string, string>
  message: string
}

export class ApiError extends Error {
  readonly status: number
  readonly reason: ErrorReason | null
  readonly retryAfter: number | null
  readonly fieldErrors: Record<string, string>

  constructor(init: ApiErrorInit) {
    super(init.message)
    this.name = 'ApiError'
    this.status = init.status
    this.reason = init.reason
    this.retryAfter = init.retryAfter
    this.fieldErrors = init.fieldErrors
  }
}

export interface ErrorMessage {
  key: string
  params: Record<string, number>
}

function toReason(value: unknown): ErrorReason | null {
  return ERROR_REASONS.find((reason) => reason === value) ?? null
}

function toRetryAfter(value: unknown): number | null {
  const seconds = Number(value)
  return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : null
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (isAxiosError<ProblemDetailDto>(error) && error.response) {
    const problem = error.response.data
    return new ApiError({
      status: error.response.status,
      reason: toReason(problem?.reason),
      retryAfter: toRetryAfter(error.response.headers['retry-after']),
      fieldErrors: problem?.errors ?? {},
      message: problem?.detail ?? error.message,
    })
  }
  return new ApiError({
    status: 0,
    reason: null,
    retryAfter: null,
    fieldErrors: {},
    message: error instanceof Error ? error.message : 'Network error',
  })
}

export function describeError(error: unknown): ErrorMessage {
  const apiError = toApiError(error)
  if (apiError.status === 0) {
    return { key: 'errors.NETWORK', params: {} }
  }
  if (apiError.reason === 'RATE_LIMITED') {
    return { key: 'errors.RATE_LIMITED', params: { seconds: apiError.retryAfter ?? 60 } }
  }
  if (apiError.reason === 'GUEST_CAPACITY_REACHED') {
    const minutes = Math.ceil((apiError.retryAfter ?? 60) / 60)
    return { key: 'errors.GUEST_CAPACITY_REACHED', params: { minutes } }
  }
  if (apiError.reason) {
    return { key: `errors.${apiError.reason}`, params: {} }
  }
  return { key: 'errors.GENERIC', params: {} }
}
