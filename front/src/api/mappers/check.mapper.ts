import type { CheckResultDto, LastCheckDto } from '../dto/check.dto'
import type { CheckResult, LastCheck } from '../api.types'

export function toLastCheck(dto: LastCheckDto): LastCheck {
  return {
    checkedAt: new Date(dto.checkedAt),
    status: dto.status,
    reason: dto.reason,
    httpStatus: dto.httpStatus,
    latencyMs: dto.latencyMs,
  }
}

export function toCheckResult(dto: CheckResultDto): CheckResult {
  return {
    ...toLastCheck(dto),
    finalUrl: dto.finalUrl,
    redirects: dto.redirects,
  }
}
