<template>
  <ul class="grid gap-3">
    <li v-for="monitor in monitors" :key="monitor.id">
      <MonitorCard
        :monitor="monitor"
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
  },
  emits: ['edit', 'delete'],
}
</script>
