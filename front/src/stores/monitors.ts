import { defineStore } from 'pinia'

import type { Monitor, MonitorChanges, MonitorDraft } from '@/api/api.types'
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

export const PAGE_SIZE = 10

let lastRequest = 0

export const useMonitorsStore = defineStore('monitors', {
  state: (): MonitorsState => ({
    items: [],
    page: null,
    ownedCount: 0,
    loading: false,
    fetching: false,
    loaded: false,
    error: null,
  }),

  getters: {
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
        const result = await listMonitors({ page: page ?? this.currentPage, size: PAGE_SIZE })
        if (request !== lastRequest) {
          return
        }
        if (!result.monitors.length && result.page.number > 0) {
          await this.fetchPage(Math.max(result.page.totalPages - 1, 0))
          return
        }
        this.items = result.monitors
        this.page = result.page
        if (result.page.number === 0) {
          this.ownedCount = result.monitors.filter((monitor) => monitor.owned).length
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

    async fetchOne(id: string): Promise<Monitor> {
      const monitor = await getMonitor(id)
      this.upsert(monitor)
      return monitor
    },

    async create(draft: MonitorDraft): Promise<Monitor> {
      const monitor = await createMonitor(draft, this.canSetPublic)
      await this.fetchPage(0)
      return monitor
    },

    async update(id: string, changes: MonitorChanges): Promise<Monitor> {
      const monitor = await updateMonitor(id, changes, this.canSetPublic)
      this.upsert(monitor)
      return monitor
    },

    async remove(id: string) {
      await deleteMonitor(id)
      await this.fetchPage(0)
    },

    async checkNow(id: string) {
      const result = await checkMonitor(id)
      const monitor = this.items.find((item) => item.id === id)
      if (monitor) {
        monitor.lastCheck = {
          checkedAt: result.checkedAt,
          status: result.status,
          reason: result.reason,
          httpStatus: result.httpStatus,
          latencyMs: result.latencyMs,
        }
      }
      return result
    },

    upsert(monitor: Monitor) {
      const index = this.items.findIndex((item) => item.id === monitor.id)
      if (index === -1) {
        this.items = [...this.items, monitor]
      } else {
        this.items.splice(index, 1, monitor)
      }
    },
  },
})
