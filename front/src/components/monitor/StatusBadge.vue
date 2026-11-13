<template>
  <span class="inline-block rounded px-2 py-0.5 text-xs font-semibold" :class="colorClass">
    {{ status ? $t(`status.${status}`) : 'Sin chequeos' }}
  </span>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { HealthStatus } from '@/api/api.types'

const COLORS: Record<HealthStatus, string> = {
  UP: 'bg-green-100 text-green-800',
  DOWN: 'bg-red-100 text-red-800',
  BLOCKED: 'bg-amber-100 text-amber-800',
  REJECTED: 'bg-gray-200 text-gray-800',
}

export default {
  name: 'StatusBadge',
  props: {
    status: {
      type: String as PropType<HealthStatus | null>,
      default: null,
    },
  },
  computed: {
    colorClass(): string {
      return this.status ? COLORS[this.status] : 'bg-gray-100 text-gray-600'
    },
  },
}
</script>
