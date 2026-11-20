<template>
  <svg
    viewBox="0 0 100 32"
    preserveAspectRatio="none"
    class="size-full overflow-visible"
    role="img"
    :aria-label="summary"
  >
    <line
      v-for="mark in marks"
      :key="`mark-${mark.key}`"
      :x1="mark.x"
      :x2="mark.x"
      y1="0"
      y2="32"
      :class="MARK_CLASS[mark.status]"
      stroke-width="2"
      vector-effect="non-scaling-stroke"
    />
    <polyline
      v-if="drawLine"
      :points="linePoints"
      fill="none"
      class="stroke-status-up"
      stroke-width="1.75"
      stroke-linejoin="round"
      stroke-linecap="round"
      vector-effect="non-scaling-stroke"
    />
    <g v-else>
      <line
        v-for="point in points"
        :key="`dot-${point.key}`"
        :x1="point.x"
        :x2="point.x"
        :y1="point.y"
        :y2="point.y"
        class="stroke-status-up"
        stroke-width="6"
        stroke-linecap="round"
        vector-effect="non-scaling-stroke"
      />
    </g>
  </svg>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { HealthStatus, RecentCheck } from '@/api/api.types'
import { RECENT_CHECKS_MAX } from '@/stores/monitors'

import type { SparkPoint } from '../components.types'

const WIDTH = 100
const HEIGHT = 32
const PADDING = 2
const STEP = WIDTH / (RECENT_CHECKS_MAX - 1)
const MIN_POINTS_FOR_LINE = 3
const FLOOR_STATUSES: HealthStatus[] = ['DOWN', 'REJECTED']

const MARK_CLASS: Record<HealthStatus, string> = {
  UP: '',
  DOWN: 'stroke-status-down/75',
  BLOCKED: 'stroke-status-blocked/50',
  REJECTED: 'stroke-status-rejected/50',
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
    MARK_CLASS: () => MARK_CLASS,
    maxLatency(): number {
      const latencies = this.checks
        .filter((check) => !FLOOR_STATUSES.includes(check.status))
        .map((check) => check.latencyMs)
      return Math.max(1, ...latencies)
    },
    points(): SparkPoint[] {
      const last = this.checks.length - 1
      const usable = HEIGHT - PADDING * 2
      return this.checks.map((check, index) => {
        const onFloor = FLOOR_STATUSES.includes(check.status)
        const ratio = onFloor ? 0 : Math.min(check.latencyMs, this.maxLatency) / this.maxLatency
        return {
          key: check.checkedAt.getTime(),
          x: WIDTH - (last - index) * STEP,
          y: HEIGHT - PADDING - ratio * usable,
          status: check.status,
        }
      })
    },
    drawLine(): boolean {
      return this.checks.length >= MIN_POINTS_FOR_LINE
    },
    linePoints(): string {
      return this.points.map((point) => `${point.x},${point.y}`).join(' ')
    },
    marks(): SparkPoint[] {
      return this.points.filter((point) => point.status !== 'UP')
    },
    summary(): string {
      const downs = this.checks.filter((check) => check.status === 'DOWN').length
      const count = this.checks.length
      return `${this.$t('monitor.sparkline.latency', count)}, ${this.$t('monitor.sparkline.downs', downs)}`
    },
  },
}
</script>
