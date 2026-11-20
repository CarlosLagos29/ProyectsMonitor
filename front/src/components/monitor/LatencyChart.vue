<template>
  <svg
    :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
    preserveAspectRatio="none"
    class="size-full overflow-visible"
    role="img"
    :aria-label="summary"
  >
    <line
      v-for="line in GRID_LINES"
      :key="`grid-${line}`"
      x1="0"
      :x2="WIDTH"
      :y1="line"
      :y2="line"
      class="stroke-primary/10"
      stroke-width="1"
      vector-effect="non-scaling-stroke"
    />
    <rect
      v-for="band in downBands"
      :key="`band-${band.key}`"
      :x="band.x"
      y="0"
      :width="band.width"
      :height="HEIGHT"
      class="fill-status-down/15"
    />
    <line
      v-for="mark in marks"
      :key="`mark-${mark.key}`"
      :x1="mark.x"
      :x2="mark.x"
      y1="0"
      :y2="HEIGHT"
      :class="MARK_CLASS[mark.status]"
      stroke-width="1.5"
      vector-effect="non-scaling-stroke"
    />
    <polyline
      v-for="(segment, index) in segments"
      :key="`line-${index}`"
      :points="segment"
      fill="none"
      class="stroke-status-up"
      stroke-width="1.5"
      stroke-linejoin="round"
      stroke-linecap="round"
      vector-effect="non-scaling-stroke"
    />
  </svg>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { CheckResult, HealthStatus } from '@/api/api.types'

import type { SparkPoint } from '../components.types'

const WIDTH = 1000
const HEIGHT = 300
const PADDING = 6
const GAP_MS = 25 * 60_000
const DEFAULT_STEP_MS = 10 * 60_000
const MIN_SCALE_MS = 100
const SCALE_PERCENTILE = 0.95
const SCALE_HEADROOM = 1.2
const GRID_LINES = [HEIGHT * 0.25, HEIGHT * 0.5, HEIGHT * 0.75]
const FLOOR_STATUSES: HealthStatus[] = ['DOWN', 'REJECTED']

const MARK_CLASS: Record<HealthStatus, string> = {
  UP: '',
  DOWN: 'stroke-status-down/75',
  BLOCKED: 'stroke-status-blocked/50',
  REJECTED: 'stroke-status-rejected/50',
}

export default {
  name: 'LatencyChart',
  props: {
    results: {
      type: Array as PropType<CheckResult[]>,
      required: true,
    },
    from: {
      type: Date,
      required: true,
    },
    to: {
      type: Date,
      required: true,
    },
  },
  computed: {
    WIDTH: () => WIDTH,
    HEIGHT: () => HEIGHT,
    GRID_LINES: () => GRID_LINES,
    MARK_CLASS: () => MARK_CLASS,
    span(): number {
      return Math.max(1, this.to.getTime() - this.from.getTime())
    },
    scale(): number {
      const latencies = this.results
        .filter((result) => !FLOOR_STATUSES.includes(result.status))
        .map((result) => result.latencyMs)
        .sort((a, b) => a - b)
      if (!latencies.length) {
        return MIN_SCALE_MS
      }
      const index = Math.min(latencies.length - 1, Math.floor(latencies.length * SCALE_PERCENTILE))
      return Math.max(MIN_SCALE_MS, (latencies[index] ?? MIN_SCALE_MS) * SCALE_HEADROOM)
    },
    points(): SparkPoint[] {
      const usable = HEIGHT - PADDING * 2
      return this.results.map((result) => {
        const onFloor = FLOOR_STATUSES.includes(result.status)
        const ratio = onFloor ? 0 : Math.min(result.latencyMs, this.scale) / this.scale
        return {
          key: result.checkedAt.getTime(),
          x: this.toX(result.checkedAt.getTime()),
          y: HEIGHT - PADDING - ratio * usable,
          status: result.status,
        }
      })
    },
    segments(): string[] {
      const segments: string[][] = []
      let current: string[] = []
      let previousTime: number | null = null
      for (const point of this.points) {
        if (previousTime !== null && point.key - previousTime > GAP_MS) {
          segments.push(current)
          current = []
        }
        current.push(`${point.x},${point.y}`)
        previousTime = point.key
      }
      segments.push(current)
      return segments.filter((segment) => segment.length > 1).map((segment) => segment.join(' '))
    },
    marks(): SparkPoint[] {
      return this.points.filter((point) => point.status !== 'UP')
    },
    downBands(): { key: number; x: number; width: number }[] {
      return this.points
        .map((point, index) => ({ point, next: this.points[index + 1] }))
        .filter(({ point }) => point.status === 'DOWN')
        .map(({ point, next }) => {
          const nextTime =
            next && next.key - point.key <= GAP_MS ? next.key : point.key + DEFAULT_STEP_MS
          return {
            key: point.key,
            x: point.x,
            width: Math.max(1, this.toX(nextTime) - point.x),
          }
        })
    },
    summary(): string {
      const count = this.results.length
      const downs = this.results.filter((result) => result.status === 'DOWN').length
      const blocked = this.results.filter((result) => result.status === 'BLOCKED').length
      return this.$t('monitorDetail.chartSummary', {
        checks: this.$t('monitor.uptime.checks', count),
        downs: this.$t('monitorDetail.downs', downs),
        blocked: this.$t('monitorDetail.blocked', blocked),
      })
    },
  },
  methods: {
    toX(time: number): number {
      const ratio = (time - this.from.getTime()) / this.span
      return Math.min(WIDTH, Math.max(0, ratio * WIDTH))
    },
  },
}
</script>
