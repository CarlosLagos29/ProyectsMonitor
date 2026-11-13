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

export const useMonitorsStore = defineStore('monitors', {
  state: (): MonitorsState => ({
    items: [],
    loading: false,
    loaded: false,
    error: null,
  }),

  getters: {
    owned: (state) => state.items.filter((monitor) => monitor.owned),
    others: (state) => state.items.filter((monitor) => !monitor.owned),
    canSetPublic: () => useSessionStore().limits?.canSetPublic ?? false,
    canCreate(): boolean {
      const limits = useSessionStore().limits
      if (!limits) {
        return false
      }
      return limits.maxMonitors === null || this.owned.length < limits.maxMonitors
    },
  },

  actions: {
    async fetchAll() {
      this.loading = !this.loaded
      try {
        this.items = await listMonitors()
        this.loaded = true
        this.error = null
      } catch (error) {
        this.error = describeError(error)
      } finally {
        this.loading = false
      }
    },

    async fetchOne(id: string): Promise<Monitor> {
      const monitor = await getMonitor(id)
      this.upsert(monitor)
      return monitor
    },

    async create(draft: MonitorDraft): Promise<Monitor> {
      const monitor = await createMonitor(draft, this.canSetPublic)
      this.items = [monitor, ...this.items]
      return monitor
    },

    async update(id: string, changes: MonitorChanges): Promise<Monitor> {
      const monitor = await updateMonitor(id, changes, this.canSetPublic)
      this.upsert(monitor)
      return monitor
    },

    async remove(id: string) {
      await deleteMonitor(id)
      this.items = this.items.filter((monitor) => monitor.id !== id)
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
