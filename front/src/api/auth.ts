import type { LoginRequestDto, MeResponseDto, SessionResponseDto } from './dto/auth.dto'
import { API_PREFIX, http } from './http'
import { toMe, toSession } from './mappers/session.mapper'
import type { Me, Session } from './api.types'

const AUTH_PATH = `${API_PREFIX}/auth`

export async function startGuest(): Promise<Session> {
  const { data } = await http.post<SessionResponseDto>(`${AUTH_PATH}/guest`)
  return toSession(data)
}

export async function login(email: string, password: string): Promise<Session> {
  const body: LoginRequestDto = { email, password }
  const { data } = await http.post<SessionResponseDto>(`${AUTH_PATH}/login`, body)
  return toSession(data)
}

export async function getMe(): Promise<Me> {
  const { data } = await http.get<MeResponseDto>(`${AUTH_PATH}/me`)
  return toMe(data)
}
