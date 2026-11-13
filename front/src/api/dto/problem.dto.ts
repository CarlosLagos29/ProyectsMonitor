export interface ProblemDetailDto {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  reason?: string
  errors?: Record<string, string>
}
