<template>
  <article class="space-y-2 rounded border p-3">
    <header class="flex items-start justify-between gap-2">
      <div class="min-w-0">
        <RouterLink
          :to="{ name: 'monitor-detail', params: { id: monitor.id } }"
          class="font-semibold underline"
        >
          {{ monitor.name }}
        </RouterLink>
        <p class="truncate text-sm text-gray-600" :title="monitor.url">{{ monitor.url }}</p>
      </div>
      <StatusBadge :status="monitor.lastCheck?.status ?? null" />
    </header>

    <div class="space-y-1 text-sm">
      <template v-if="monitor.lastCheck">
        <p>
          {{ $t(`checkReason.${monitor.lastCheck.reason}`) }}
          <span v-if="monitor.lastCheck.status === 'UP'"
            >· {{ monitor.lastCheck.latencyMs }} ms</span
          >
          · {{ lastCheckedText }}
        </p>
        <p v-if="hint" class="text-gray-600">{{ hint }}</p>
      </template>
      <p v-else class="text-gray-600">Sin chequeos todavía.</p>
      <p>{{ uptimeText }}</p>
    </div>

    <ul class="flex flex-wrap gap-2 text-xs">
      <li v-if="!monitor.enabled" class="rounded bg-gray-200 px-2 py-0.5">Pausado</li>
      <li v-if="canSetPublic && monitor.owned" class="rounded bg-gray-100 px-2 py-0.5">
        {{ monitor.isPublic ? 'Público' : 'Privado' }}
      </li>
      <li v-if="!monitor.owned" class="rounded bg-gray-100 px-2 py-0.5">Solo lectura</li>
      <li v-if="expiresText" class="rounded bg-amber-100 px-2 py-0.5">{{ expiresText }}</li>
    </ul>

    <div v-if="monitor.owned" class="flex flex-wrap gap-2">
      <button
        type="button"
        class="rounded border px-2 py-1 text-sm"
        :disabled="checking || cooldownSeconds > 0"
        @click="runCheck"
      >
        {{ checkButtonText }}
      </button>
      <button
        type="button"
        class="rounded border px-2 py-1 text-sm"
        :disabled="toggling"
        @click="toggleEnabled"
      >
        {{ monitor.enabled ? 'Pausar' : 'Reanudar' }}
      </button>
      <button
        type="button"
        class="rounded border px-2 py-1 text-sm"
        @click="$emit('edit', monitor)"
      >
        Editar
      </button>
      <button
        type="button"
        class="rounded border px-2 py-1 text-sm"
        @click="$emit('delete', monitor)"
      >
        Borrar
      </button>
    </div>
    <p v-if="actionError" role="alert" class="text-sm text-red-600">
      {{ $t(actionError.key, actionError.params) }}
    </p>
  </article>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { mapActions, mapState } from 'pinia'

import type { Monitor } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'
import { formatElapsed, formatRemaining } from '@/utils/time'

import type { MonitorCardData } from '../components.types'
import StatusBadge from './StatusBadge.vue'

const TICK_MS = 1_000

export default {
  name: 'MonitorCard',
  components: { StatusBadge },
  props: {
    monitor: {
      type: Object as PropType<Monitor>,
      required: true,
    },
  },
  emits: ['edit', 'delete'],
  data(): MonitorCardData {
    return {
      now: Date.now(),
      ticker: undefined,
      checking: false,
      toggling: false,
      cooldownUntil: 0,
      actionError: null,
    }
  },
  computed: {
    ...mapState(useSessionStore, ['limits']),
    ...mapState(useMonitorsStore, ['canSetPublic']),
    lastCheckedText(): string {
      return this.monitor.lastCheck ? formatElapsed(this.monitor.lastCheck.checkedAt, this.now) : ''
    },
    hint(): string {
      const status = this.monitor.lastCheck?.status
      const reason = this.monitor.lastCheck?.reason
      if (reason === 'TIMEOUT') {
        return 'Puede ser una app dormida en un hosting gratuito: tarda en despertar.'
      }
      if (status === 'BLOCKED') {
        return 'El sitio no deja pasar al checker, así que no se sabe si está sano.'
      }
      return ''
    },
    uptimeText(): string {
      const { percentage, checks } = this.monitor.uptime
      if (percentage === null) {
        return 'Sin chequeos en las últimas 24 h.'
      }
      return `Uptime 24 h: ${percentage}% (basado en ${checks} ${checks === 1 ? 'chequeo' : 'chequeos'})`
    },
    expiresText(): string {
      if (!this.monitor.owned || !this.monitor.expiresAt) {
        return ''
      }
      return `Se borra en ${formatRemaining(this.monitor.expiresAt.getTime() - this.now)}`
    },
    cooldownSeconds(): number {
      return Math.max(0, Math.ceil((this.cooldownUntil - this.now) / 1_000))
    },
    checkButtonText(): string {
      if (this.checking) {
        return 'Chequeando…'
      }
      if (this.cooldownSeconds > 0) {
        return `Chequear en ${this.cooldownSeconds} s`
      }
      return 'Chequear ahora'
    },
  },
  mounted() {
    this.ticker = setInterval(() => this.tick(), TICK_MS)
  },
  beforeUnmount() {
    clearInterval(this.ticker)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['checkNow', 'update', 'fetchPage']),
    tick() {
      this.now = Date.now()
      if (this.monitor.expiresAt && this.monitor.expiresAt.getTime() <= this.now) {
        clearInterval(this.ticker)
        void this.fetchPage()
      }
    },
    async runCheck() {
      this.actionError = null
      this.checking = true
      try {
        await this.checkNow(this.monitor.id)
        this.startCooldown(this.limits?.manualCheckCooldownSeconds ?? 60)
      } catch (error) {
        this.handleError(error)
      } finally {
        this.checking = false
      }
    },
    async toggleEnabled() {
      this.actionError = null
      this.toggling = true
      try {
        await this.update(this.monitor.id, { enabled: !this.monitor.enabled })
      } catch (error) {
        this.handleError(error)
      } finally {
        this.toggling = false
      }
    },
    startCooldown(seconds: number) {
      this.cooldownUntil = Date.now() + seconds * 1_000
      this.now = Date.now()
    },
    handleError(error: unknown) {
      const apiError = toApiError(error)
      if (apiError.reason === 'RATE_LIMITED' && apiError.retryAfter) {
        this.startCooldown(apiError.retryAfter)
      }
      this.actionError = describeError(apiError)
    },
  },
}
</script>
