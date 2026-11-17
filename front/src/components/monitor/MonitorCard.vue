<template>
  <article class="panel flex h-full min-w-0 flex-col">
    <header class="flex items-center gap-3 border-b border-border px-4 py-3">
      <span class="cam-label shrink-0 text-muted-foreground">{{ camLabel }}</span>
      <RouterLink
        :to="{ name: 'monitor-detail', params: { id: monitor.id } }"
        class="min-w-0 flex-1 truncate text-[17px] font-semibold underline decoration-border underline-offset-4 hover:decoration-primary"
        @click="setCurrentCam(cam)"
      >
        {{ monitor.name }}
      </RouterLink>
      <StatusBadge :status="status" />
    </header>

    <div class="p-4 pb-0">
      <CameraFeed class="h-37.5 min-[720px]:h-42.5" :noise="feedNoise">
        <div v-if="monitor.recentChecks.length" class="absolute inset-x-3 top-9 bottom-9">
          <MonitorSparkline :checks="monitor.recentChecks" />
        </div>
        <template #top-left>
          <span :class="recording ? 'rec-dot' : 'rec-dot-off'" aria-hidden="true" />
          {{ feedTag }}
        </template>
        <template v-if="monitor.lastCheck" #top-right>
          {{ lastCheckStamp }}
        </template>
        <template #bottom-left>−24 H</template>
        <template v-if="latencyText" #bottom-right>{{ latencyText }}</template>
        <template v-if="feedMessage" #message>{{ feedMessage }}</template>
      </CameraFeed>
    </div>

    <div class="flex flex-1 flex-col gap-3 p-4">
      <p class="truncate font-mono text-[13px] text-muted-foreground" :title="monitor.url">
        {{ monitor.url }}
      </p>
      <div v-if="monitor.lastCheck" class="space-y-1 text-sm">
        <p>
          <span class="text-muted-foreground">Motivo ·</span>
          {{ $t(`checkReason.${monitor.lastCheck.reason}`) }}
        </p>
        <p v-if="hint" class="text-xs text-muted-foreground">{{ hint }}</p>
      </div>
      <p v-else class="text-sm text-muted-foreground">
        Todavía no hay chequeos: el primero llega en menos de un minuto.
      </p>

      <div class="mt-auto flex flex-wrap items-end justify-between gap-3">
        <UptimeBattery :percentage="monitor.uptime.percentage" :checks="monitor.uptime.checks" />
        <div class="flex min-w-0 flex-col items-end gap-2">
          <p v-if="monitor.lastCheck" class="text-xs text-muted-foreground">
            Último chequeo: {{ lastCheckedText }}
          </p>
          <ul class="flex flex-wrap justify-end gap-1.5">
            <li v-if="!monitor.enabled" class="chip">
              <Pause :size="12" aria-hidden="true" />
              Pausado
            </li>
            <li v-if="canSetPublic && monitor.owned" class="chip">
              <Globe v-if="monitor.isPublic" :size="12" aria-hidden="true" />
              <Lock v-else :size="12" aria-hidden="true" />
              {{ monitor.isPublic ? 'Público' : 'Privado' }}
            </li>
            <li v-if="isAdmin && !monitor.owned" class="chip">
              <User :size="12" aria-hidden="true" />
              De un invitado
            </li>
            <li v-if="!canManage" class="chip">Solo lectura</li>
            <li v-if="expiresText" class="chip border-warning/60 text-warning">
              <Clock :size="12" aria-hidden="true" />
              {{ expiresText }}
            </li>
          </ul>
        </div>
      </div>

      <p v-if="actionError" role="alert" class="field-error">
        <CircleAlert :size="14" aria-hidden="true" />
        {{ $t(actionError.key, actionError.params) }}
      </p>
    </div>

    <footer v-if="canManage" class="flex gap-2 border-t border-border p-3">
      <CooldownButton
        class="min-w-0 flex-1"
        :checking="checking"
        :cooldown-seconds="cooldownSeconds"
        @check="runCheck"
      />
      <button
        type="button"
        class="btn btn-secondary btn-icon"
        :aria-label="monitor.enabled ? 'Pausar' : 'Reanudar'"
        :title="monitor.enabled ? 'Pausar' : 'Reanudar'"
        :disabled="toggling"
        @click="toggleEnabled"
      >
        <Pause v-if="monitor.enabled" :size="18" aria-hidden="true" />
        <Play v-else :size="18" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="btn btn-secondary btn-icon"
        aria-label="Editar"
        title="Editar"
        @click="$emit('edit', monitor)"
      >
        <Pencil :size="18" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="btn btn-danger btn-icon"
        aria-label="Borrar"
        title="Borrar"
        @click="$emit('delete', monitor)"
      >
        <Trash2 :size="18" aria-hidden="true" />
      </button>
    </footer>
  </article>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { CircleAlert, Clock, Globe, Lock, Pause, Pencil, Play, Trash2, User } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import type { HealthStatus, Monitor } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import CameraFeed from '@/components/layout/CameraFeed.vue'
import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'
import { formatElapsed, formatRemaining, formatStamp } from '@/utils/time'

import type { MonitorCardData } from '../components.types'
import CooldownButton from './CooldownButton.vue'
import MonitorSparkline from './MonitorSparkline.vue'
import StatusBadge from './StatusBadge.vue'
import UptimeBattery from './UptimeBattery.vue'

const TICK_MS = 1_000
const NOISE_DOWN = 0.32
const NOISE_WAITING = 0.18

export default {
  name: 'MonitorCard',
  components: {
    CameraFeed,
    CircleAlert,
    Clock,
    CooldownButton,
    Globe,
    Lock,
    MonitorSparkline,
    Pause,
    Pencil,
    Play,
    StatusBadge,
    Trash2,
    UptimeBattery,
    User,
  },
  props: {
    monitor: {
      type: Object as PropType<Monitor>,
      required: true,
    },
    cam: {
      type: Number as PropType<number | null>,
      default: null,
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
    ...mapState(useSessionStore, ['limits', 'isAdmin']),
    ...mapState(useMonitorsStore, ['canSetPublic']),
    canManage(): boolean {
      return this.isAdmin || this.monitor.owned
    },
    camLabel(): string {
      return this.cam === null ? 'CAM ??' : `CAM ${String(this.cam).padStart(2, '0')}`
    },
    status(): HealthStatus | null {
      return this.monitor.lastCheck?.status ?? null
    },
    recording(): boolean {
      return this.status === 'UP' || this.status === 'DOWN' || this.status === 'BLOCKED'
    },
    feedTag(): string {
      if (this.status === 'REJECTED') {
        return 'Sin acceso'
      }
      if (this.status === null) {
        return 'Conectando'
      }
      return 'REC'
    },
    feedMessage(): string {
      if (this.status === 'DOWN') {
        return 'Señal perdida'
      }
      if (this.status === 'REJECTED') {
        return 'Acceso denegado'
      }
      if (this.status === null) {
        return 'Conectando…'
      }
      return ''
    },
    feedNoise(): number {
      if (this.status === 'DOWN') {
        return NOISE_DOWN
      }
      if (this.status === null) {
        return NOISE_WAITING
      }
      return 0
    },
    lastCheckStamp(): string {
      return this.monitor.lastCheck ? formatStamp(this.monitor.lastCheck.checkedAt) : ''
    },
    latencyText(): string {
      const lastCheck = this.monitor.lastCheck
      if (!lastCheck || lastCheck.status === 'DOWN' || lastCheck.status === 'REJECTED') {
        return ''
      }
      return `LAT ${lastCheck.latencyMs} ms`
    },
    lastCheckedText(): string {
      return this.monitor.lastCheck ? formatElapsed(this.monitor.lastCheck.checkedAt, this.now) : ''
    },
    hint(): string {
      if (this.monitor.lastCheck?.reason === 'TIMEOUT') {
        return 'Puede ser una app dormida en un hosting gratuito: tarda en despertar.'
      }
      if (this.status === 'BLOCKED') {
        return 'El sitio no deja pasar al checker, así que no se sabe si está sano.'
      }
      return ''
    },
    expiresText(): string {
      if (!this.monitor.expiresAt) {
        return ''
      }
      return `Se borra en ${formatRemaining(this.monitor.expiresAt.getTime() - this.now)}`
    },
    cooldownSeconds(): number {
      return Math.max(0, Math.ceil((this.cooldownUntil - this.now) / 1_000))
    },
  },
  mounted() {
    this.ticker = setInterval(() => this.tick(), TICK_MS)
  },
  beforeUnmount() {
    clearInterval(this.ticker)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['checkNow', 'update', 'fetchPage', 'setCurrentCam']),
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
