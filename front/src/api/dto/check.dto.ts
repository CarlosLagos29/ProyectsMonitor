export type HealthStatusDto = 'UP' | 'DOWN' | 'BLOCKED' | 'REJECTED'

export type HealthReasonDto =
  | 'SUCCESS'
  | 'TIMEOUT'
  | 'DNS_ERROR'
  | 'CONNECTION_ERROR'
  | 'TLS_ERROR'
  | 'HTTP_4XX'
  | 'HTTP_5XX'
  | 'TOO_MANY_REDIRECTS'
  | 'AUTH_REQUIRED'
  | 'ACCESS_DENIED'
  | 'PRIVATE_ADDRESS'
  | 'PORT_NOT_ALLOWED'

export interface LastCheckDto {
  checkedAt: string
  status: HealthStatusDto
  reason: HealthReasonDto
  message: string
  httpStatus: number
  latencyMs: number
}

export interface RecentCheckDto {
  checkedAt: string
  status: HealthStatusDto
  latencyMs: number
}

export interface CheckResultDto extends LastCheckDto {
  finalUrl: string
  redirects: number
}

export interface CheckResultsResponseDto {
  from: string
  to: string
  results: CheckResultDto[]
}

export interface UptimeDto {
  percentage: number | null
  checks: number
}
