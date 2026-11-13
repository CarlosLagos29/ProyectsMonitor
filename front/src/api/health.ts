import { http } from './http'

interface HealthResponseDto {
  status: string
}

export async function isServerUp(): Promise<boolean> {
  try {
    const { data } = await http.get<HealthResponseDto>('/actuator/health', { timeout: 5_000 })
    return data.status === 'UP'
  } catch {
    return false
  }
}
