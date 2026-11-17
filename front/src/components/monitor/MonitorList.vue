<template>
  <ul class="grid gap-5 min-[720px]:grid-cols-2">
    <li v-for="(monitor, index) in monitors" :key="monitor.id" class="min-w-0">
      <MonitorCard
        :monitor="monitor"
        :cam="offset + index + 1"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </li>
  </ul>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { Monitor } from '@/api/api.types'

import MonitorCard from './MonitorCard.vue'

export default {
  name: 'MonitorList',
  components: { MonitorCard },
  props: {
    monitors: {
      type: Array as PropType<Monitor[]>,
      required: true,
    },
    offset: {
      type: Number,
      default: 0,
    },
  },
  emits: ['edit', 'delete'],
}
</script>
