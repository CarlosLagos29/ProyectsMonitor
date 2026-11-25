<template>
  <li class="space-y-1.5 px-3 py-2.5 odd:bg-background even:bg-muted">
    <div class="flex items-center justify-between gap-2">
      <span class="font-mono tabular-nums">{{ formatStamp(result.checkedAt) }}</span>
      <StatusBadge :status="result.status" small />
    </div>
    <div class="flex items-center justify-between gap-2 text-muted-foreground">
      <span class="min-w-0">
        <span v-if="result.httpStatus > 0" class="font-mono">{{ result.httpStatus }} · </span>
        {{ $t(`checkReason.${result.reason}`) }}
      </span>
      <span class="shrink-0 font-mono tabular-nums">{{ formatCheckLatency(result) }}</span>
    </div>
  </li>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { CheckResult } from '@/api/api.types'
import { formatCheckLatency } from '@/utils/status'
import { formatStamp } from '@/utils/time'

import StatusBadge from './StatusBadge.vue'

export default {
  name: 'ChecksListItem',
  components: { StatusBadge },
  props: {
    result: {
      type: Object as PropType<CheckResult>,
      required: true,
    },
  },
  methods: {
    formatStamp,
    formatCheckLatency,
  },
}
</script>
