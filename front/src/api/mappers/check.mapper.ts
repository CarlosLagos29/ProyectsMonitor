import type {
  CheckResultDto,
  CheckResultsResponseDto,
  LastCheckDto,
  RecentCheckDto,
} from '../dto/check.dto'
import type { CheckResult, CheckSeries, LastCheck, RecentCheck } from '../api.types'

export function toLastCheck(dto: LastCheckDto): LastCheck {
  return {
    checkedAt: new Date(dto.checkedAt),
    status: dto.status,
    reason: dto.reason,
    httpStatus: dto.httpStatus,
    latencyMs: dto.latencyMs,
  }
}

export function toRecentCheck(dto: RecentCheckDto): RecentCheck {
  return {
    checkedAt: new Date(dto.checkedAt),
    status: dto.status,
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

export function toCheckSeries(dto: CheckResultsResponseDto): CheckSeries {
  return {
    from: new Date(dto.from),
    to: new Date(dto.to),
    results: dto.results.map(toCheckResult),
  }
}
