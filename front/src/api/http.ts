import axios from 'axios'

import { toApiError } from './errors'

export const API_PREFIX = '/api/v1'

const API_TOKEN_PATHS = [`${API_PREFIX}/auth/guest`, `${API_PREFIX}/auth/login`]

interface HttpHooks {
  getToken: () => string | null
  onSessionExpired: () => void
}

let hooks: HttpHooks = {
  getToken: () => null,
  onSessionExpired: () => undefined,
}

export function configureHttp(next: HttpHooks) {
  hooks = next
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15_000,
})

http.interceptors.request.use((config) => {
  const url = config.url ?? ''
  const token = API_TOKEN_PATHS.includes(url)
    ? import.meta.env.VITE_API_TOKEN
    : url.startsWith(API_PREFIX)
      ? hooks.getToken()
      : null
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(undefined, (error: unknown) => {
  const apiError = toApiError(error)
  const url = axios.isAxiosError(error) ? (error.config?.url ?? '') : ''
  if (apiError.reason === 'SESSION_EXPIRED' && !API_TOKEN_PATHS.includes(url)) {
    hooks.onSessionExpired()
  }
  return Promise.reject(apiError)
})
