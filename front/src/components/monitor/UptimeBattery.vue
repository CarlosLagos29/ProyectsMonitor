<template>
  <div class="flex items-center gap-3">
    <div class="flex items-center" role="img" :aria-label="ariaLabel">
      <div class="flex h-6 gap-0.5 rounded-[3px] border-2 border-foreground p-0.5">
        <span
          v-for="bar in BARS"
          :key="bar"
          class="w-2.5 rounded-[1px]"
          :class="bar <= litBars ? barColor : 'bg-border'"
        />
      </div>
      <span class="h-2.5 w-1 rounded-r-[1px] bg-foreground" />
    </div>
    <div class="min-w-0">
      <p class="font-cam text-[24px] leading-none">{{ valueText }}</p>
      <p class="text-xs text-muted-foreground">{{ detailText }}</p>
    </div>
  </div>
</template>

<script lang="ts">
import type { PropType } from 'vue'

const BARS = [1, 2, 3, 4, 5]

export default {
  name: 'UptimeBattery',
  props: {
    percentage: {
      type: Number as PropType<number | null>,
      default: null,
    },
    checks: {
      type: Number,
      default: 0,
    },
  },
  computed: {
    BARS: () => BARS,
    value(): number {
      return this.percentage ?? 0
    },
    hasData(): boolean {
      return this.percentage !== null && this.checks > 0
    },
    litBars(): number {
      return this.hasData ? Math.round(this.value / 20) : 0
    },
    barColor(): string {
      if (this.value >= 95) {
        return 'bg-status-up'
      }
      if (this.value >= 90) {
        return 'bg-status-blocked'
      }
      return 'bg-status-down'
    },
    valueText(): string {
      if (!this.hasData) {
        return '—'
      }
      const formatted = new Intl.NumberFormat(this.$i18n.locale, {
        maximumFractionDigits: 1,
      }).format(this.value)
      return `${formatted}%`
    },
    detailText(): string {
      if (!this.hasData) {
        return 'uptime 24 h · sin chequeos'
      }
      return `uptime 24 h · ${this.checks} ${this.checks === 1 ? 'chequeo' : 'chequeos'}`
    },
    ariaLabel(): string {
      if (!this.hasData) {
        return 'Uptime de las últimas 24 h: sin chequeos todavía'
      }
      return `Uptime de las últimas 24 h: ${this.valueText}, basado en ${this.checks} ${this.checks === 1 ? 'chequeo' : 'chequeos'}`
    },
  },
}
</script>
