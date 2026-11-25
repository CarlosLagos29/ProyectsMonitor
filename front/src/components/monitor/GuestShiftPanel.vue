<template>
  <aside class="panel flex flex-col justify-center gap-4 p-5">
    <h2 class="cam-label text-[24px] text-muted-foreground">{{ $t('monitor.shift.title') }}</h2>
    <p
      role="timer"
      :aria-label="$t('monitor.shift.remainingLabel', { time: remainingText })"
      class="space-y-1"
    >
      <span
        class="block font-cam text-[56px] leading-none text-warning tabular-nums sm:text-[88px]"
      >
        {{ remainingText }}
      </span>
      <span class="cam-label text-warning">{{ $t('monitor.shift.remaining') }}</span>
    </p>
    <div
      class="h-2 overflow-hidden rounded-full bg-border"
      role="progressbar"
      :aria-label="$t('monitor.shift.progress')"
      :aria-valuenow="remainingPercent"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <div class="h-full bg-warning" :style="{ width: `${remainingPercent}%` }" />
    </div>
    <p class="text-sm">
      {{ $t('monitor.shift.lifetime', { created: createdText, expires: expiresText }) }}
    </p>
  </aside>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { Monitor } from '@/api/api.types'
import { formatClock, formatHoursMinutesSeconds } from '@/utils/time'

import type { GuestShiftData } from '../components.types'

const TICK_MS = 1_000

export default {
  name: 'GuestShiftPanel',
  props: {
    monitor: {
      type: Object as PropType<Monitor>,
      required: true,
    },
  },
  data(): GuestShiftData {
    return {
      now: Date.now(),
      ticker: undefined,
    }
  },
  computed: {
    expiresAt(): number {
      return this.monitor.expiresAt?.getTime() ?? this.now
    },
    remainingText(): string {
      return formatHoursMinutesSeconds(this.expiresAt - this.now)
    },
    remainingPercent(): number {
      const total = this.expiresAt - this.monitor.createdAt.getTime()
      if (total <= 0) {
        return 0
      }
      const remaining = Math.max(0, this.expiresAt - this.now)
      return Math.round((remaining / total) * 100)
    },
    createdText(): string {
      return formatClock(this.monitor.createdAt)
    },
    expiresText(): string {
      return formatClock(new Date(this.expiresAt))
    },
  },
  mounted() {
    this.ticker = setInterval(() => {
      this.now = Date.now()
    }, TICK_MS)
  },
  beforeUnmount() {
    clearInterval(this.ticker)
  },
}
</script>
