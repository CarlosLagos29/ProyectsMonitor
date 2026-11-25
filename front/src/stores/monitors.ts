import { defineStore } from 'pinia'

import type { Monitor, MonitorChanges, MonitorDraft, MonitorScope } from '@/api/api.types'
import { describeError } from '@/api/errors'
import {
  checkMonitor,
  createMonitor,
  deleteMonitor,
  getMonitor,
  listMonitors,
  updateMonitor,
} from '@/api/monitors'

import { useSessionStore } from './session'
import type { MonitorsState } from './stores.types'

export const PAGE_SIZE = 6

let lastRequest = 0

export const useMonitorsStore = defineStore('monitors', {
  state: (): MonitorsState => ({
    items: [],
    scope: null,
    currentCam: null,
    detail: null,
    page: null,
    ownedCount: 0,
    loading: false,
    fetching: false,
    loaded: false,
    error: null,
  }),

  getters: {
    activeScope: (state): MonitorScope =>
      state.scope ?? (useSessionStore().isGuest ? 'PUBLIC' : 'OWN'),
    currentPage: (state) => state.page?.number ?? 0,
    totalPages: (state) => state.page?.totalPages ?? 0,
    canSetPublic: () => useSessionStore().limits?.canSetPublic ?? false,
    canCreate(): boolean {
      const limits = useSessionStore().limits
      if (!limits) {
        return false
      }
      return limits.maxMonitors === null || this.ownedCount < limits.maxMonitors
    },
  },

  actions: {
    async fetchPage(page?: number) {
      const request = ++lastRequest
      this.loading = !this.loaded
      this.fetching = true
      try {
        const result = await listMonitors({
          page: page ?? this.currentPage,
          size: PAGE_SIZE,
          scope: this.activeScope,
        })
        if (request !== lastRequest) {
          return
        }
        if (!result.monitors.length && result.page.number > 0) {
          await this.fetchPage(Math.max(result.page.totalPages - 1, 0))
          return
        }
        this.items = result.monitors
        this.page = result.page
        if (this.activeScope === 'OWN') {
          this.ownedCount = result.page.totalElements
        }
        this.loaded = true
        this.error = null
      } catch (error) {
        if (request === lastRequest) {
          this.error = describeError(error)
        }
      } finally {
        if (request === lastRequest) {
          this.loading = false
          this.fetching = false
        }
      }
    },

    setCurrentCam(cam: number | null) {
      this.currentCam = cam
    },

    async setScope(scope: MonitorScope) {
      if (scope === this.activeScope) {
        return
      }
      this.scope = scope
      this.items = []
      this.page = null
      this.loaded = false
      await this.fetchPage(0)
    },

    async fetchDetail(id: string): Promise<Monitor> {
      const monitor = await getMonitor(id)
      this.detail = monitor
      this.replace(monitor)
      return monitor
    },

    async create(draft: MonitorDraft): Promise<Monitor> {
      const monitor = await createMonitor(draft, this.canSetPublic)
      await this.fetchPage(0)
      return monitor
    },

    async update(id: string, changes: MonitorChanges): Promise<Monitor> {
      const monitor = await updateMonitor(id, changes, this.canSetPublic)
      this.replace(monitor)
      return monitor
    },

    async remove(id: string) {
      await deleteMonitor(id)
    },

    async checkNow(id: string) {
      const result = await checkMonitor(id)
      try {
        this.replace(await getMonitor(id))
      } catch (error) {
        console.error(error)
      }
      return result
    },

    replace(monitor: Monitor) {
      const index = this.items.findIndex((item) => item.id === monitor.id)
      if (index !== -1) {
        this.items.splice(index, 1, monitor)
      }
      if (this.detail?.id === monitor.id) {
        this.detail = monitor
      }
    },
  },
})
