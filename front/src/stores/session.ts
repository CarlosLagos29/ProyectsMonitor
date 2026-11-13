import { defineStore } from 'pinia'

import { getMe, login as requestLogin, startGuest as requestGuest } from '@/api/auth'
import type { Session } from '@/api/api.types'

import { useMonitorsStore } from './monitors'
import type { ExitReason, SessionState, StoredSession } from './stores.types'

const STORAGE_KEY = 'session'

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }
    const stored = JSON.parse(raw) as StoredSession
    const expiresAt = new Date(stored.expiresAt)
    if (!stored.token || !stored.user || !(expiresAt.getTime() > Date.now())) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return { token: stored.token, expiresAt, user: stored.user }
  } catch {
    return null
  }
}

function writeStoredSession(session: Session | null) {
  try {
    if (session) {
      const stored: StoredSession = {
        token: session.token,
        expiresAt: session.expiresAt.toISOString(),
        user: session.user,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(stored))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    return
  }
}

let expiryTimer: ReturnType<typeof setTimeout> | undefined

export const useSessionStore = defineStore('session', {
  state: (): SessionState => {
    const stored = readStoredSession()
    return {
      token: stored?.token ?? null,
      user: stored?.user ?? null,
      expiresAt: stored?.expiresAt ?? null,
      limits: null,
      exitReason: null,
    }
  },

  getters: {
    isActive: (state) => state.token !== null,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isGuest: (state) => state.user?.role === 'GUEST',
  },

  actions: {
    async login(email: string, password: string) {
      this.apply(await requestLogin(email, password))
      await this.loadMe()
    },

    async startGuest() {
      this.apply(await requestGuest())
      await this.loadMe()
    },

    async loadMe() {
      const me = await getMe()
      this.user = me.user
      this.expiresAt = me.expiresAt
      this.limits = me.limits
    },

    logout() {
      this.clear()
      this.exitReason = null
    },

    expire() {
      if (!this.token) {
        return
      }
      const reason: ExitReason = this.isGuest ? 'expiredGuest' : 'expiredAdmin'
      this.clear()
      this.exitReason = reason
    },

    apply(session: Session) {
      this.token = session.token
      this.user = session.user
      this.expiresAt = session.expiresAt
      this.exitReason = null
      writeStoredSession(session)
      this.scheduleExpiry()
    },

    clear() {
      clearTimeout(expiryTimer)
      this.token = null
      this.user = null
      this.expiresAt = null
      this.limits = null
      writeStoredSession(null)
      useMonitorsStore().$reset()
    },

    scheduleExpiry() {
      clearTimeout(expiryTimer)
      if (!this.expiresAt) {
        return
      }
      const remaining = this.expiresAt.getTime() - Date.now()
      if (remaining <= 0) {
        this.expire()
        return
      }
      expiryTimer = setTimeout(() => this.expire(), remaining)
    },
  },
})
