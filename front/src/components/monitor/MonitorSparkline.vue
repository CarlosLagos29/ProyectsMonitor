<template>
  <p v-if="!checks.length" class="text-xs text-gray-500">Sin chequeos recientes.</p>
  <figure v-else class="space-y-1">
    <svg
      :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
      class="h-10 w-full max-w-xs"
      role="img"
      :aria-label="summary"
    >
      <polyline
        v-for="(segment, index) in segments"
        :key="`line-${index}`"
        :points="segment"
        fill="none"
        class="stroke-green-600"
        stroke-width="1.5"
        stroke-linejoin="round"
        stroke-linecap="round"
      />
      <circle
        v-for="point in dots"
        :key="`dot-${point.key}`"
        :cx="point.x"
        :cy="point.y"
        r="2"
        class="fill-green-600"
      />
      <rect
        v-for="point in issues"
        :key="`issue-${point.key}`"
        :x="point.x - 1.5"
        :y="PADDING"
        width="3"
        :height="HEIGHT - PADDING * 2"
        rx="1"
        :class="ISSUE_COLORS[point.status]"
      />
      <circle
        v-for="point in points"
        :key="`hit-${point.key}`"
        :cx="point.x"
        :cy="HEIGHT / 2"
        r="5"
        fill="transparent"
      >
        <title>{{ point.label }}</title>
      </circle>
    </svg>
    <figcaption class="text-xs text-gray-500">{{ caption }}</figcaption>
  </figure>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { HealthStatus, RecentCheck } from '@/api/api.types'
import { RECENT_CHECKS_MAX } from '@/stores/monitors'

import type { SparkPoint } from '../components.types'

const WIDTH = 240
const HEIGHT = 40
const PADDING = 4
const MIN_POINTS_FOR_LINE = 3
const STEP = (WIDTH - PADDING * 2) / (RECENT_CHECKS_MAX - 1)

const ISSUE_COLORS: Record<HealthStatus, string> = {
  UP: 'fill-green-600',
  DOWN: 'fill-red-500',
  BLOCKED: 'fill-amber-500',
  REJECTED: 'fill-gray-400',
}

export default {
  name: 'MonitorSparkline',
  props: {
    checks: {
      type: Array as PropType<RecentCheck[]>,
      required: true,
    },
  },
  computed: {
    WIDTH: () => WIDTH,
    HEIGHT: () => HEIGHT,
    PADDING: () => PADDING,
    ISSUE_COLORS: () => ISSUE_COLORS,
    upChecks(): RecentCheck[] {
      return this.checks.filter((check) => check.status === 'UP')
    },
    maxLatency(): number {
      return Math.max(1, ...this.upChecks.map((check) => check.latencyMs))
    },
    minLatency(): number {
      return this.upChecks.length ? Math.min(...this.upChecks.map((check) => check.latencyMs)) : 0
    },
    points(): SparkPoint[] {
      const last = this.checks.length - 1
      const usable = HEIGHT - PADDING * 2
      return this.checks.map((check, index) => ({
        key: check.checkedAt.getTime(),
        x: WIDTH - PADDING - (last - index) * STEP,
        y:
          HEIGHT -
          PADDING -
          (Math.min(check.latencyMs, this.maxLatency) / this.maxLatency) * usable,
        status: check.status,
        label: `${check.checkedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} · ${this.$t(`status.${check.status}`)}${check.status === 'UP' ? ` · ${check.latencyMs} ms` : ''}`,
      }))
    },
    runs(): SparkPoint[][] {
      const runs: SparkPoint[][] = []
      let current: SparkPoint[] = []
      for (const point of this.points) {
        if (point.status === 'UP') {
          current.push(point)
        } else if (current.length) {
          runs.push(current)
          current = []
        }
      }
      if (current.length) {
        runs.push(current)
      }
      return runs
    },
    drawLines(): boolean {
      return this.checks.length >= MIN_POINTS_FOR_LINE
    },
    segments(): string[] {
      if (!this.drawLines) {
        return []
      }
      return this.runs
        .filter((run) => run.length > 1)
        .map((run) => run.map((point) => `${point.x},${point.y}`).join(' '))
    },
    dots(): SparkPoint[] {
      if (!this.drawLines) {
        return this.points.filter((point) => point.status === 'UP')
      }
      return this.runs.filter((run) => run.length === 1).flat()
    },
    issues(): SparkPoint[] {
      return this.points.filter((point) => point.status !== 'UP')
    },
    caption(): string {
      const count = this.checks.length
      const base = `${count === 1 ? 'Último chequeo' : `Últimos ${count} chequeos`}`
      if (!this.upChecks.length) {
        return base
      }
      return `${base} · ${this.minLatency}–${this.maxLatency} ms`
    },
    summary(): string {
      const problems = this.issues.length
      const problemsText = problems ? `${problems} con problemas` : 'todos respondieron bien'
      return `${this.caption}: ${problemsText}.`
    },
  },
}
</script>
