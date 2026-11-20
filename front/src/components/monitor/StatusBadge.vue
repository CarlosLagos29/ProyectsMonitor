<template>
  <span
    class="inline-flex shrink-0 items-center gap-1.5 rounded-[3px] border px-2 font-cam leading-none uppercase"
    :class="[colorClass, small ? 'py-0.5 text-[18px]' : 'py-1 text-[20px]']"
  >
    <component :is="icon" :size="14" aria-hidden="true" />
    {{ label }}
  </span>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { HealthStatus } from '@/api/api.types'
import { STATUS_ICON, STATUS_TEXT_CLASS, toStatusKey } from '@/utils/status'

export default {
  name: 'StatusBadge',
  props: {
    status: {
      type: String as PropType<HealthStatus | null>,
      default: null,
    },
    small: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    colorClass(): string {
      return STATUS_TEXT_CLASS[toStatusKey(this.status)]
    },
    icon() {
      return STATUS_ICON[toStatusKey(this.status)]
    },
    label(): string {
      return this.status ? this.$t(`status.${this.status}`) : this.$t('monitor.noChecks')
    },
  },
}
</script>
