<template>
  <tr class="border-t border-border odd:bg-background even:bg-muted">
    <td class="px-4 py-2 font-mono whitespace-nowrap tabular-nums">
      {{ formatStamp(result.checkedAt) }}
    </td>
    <td class="px-4 py-2">
      <StatusBadge :status="result.status" small />
    </td>
    <td class="px-4 py-2 text-right font-mono whitespace-nowrap tabular-nums">
      {{ formatCheckLatency(result) }}
    </td>
    <td class="px-4 py-2">
      <span v-if="result.httpStatus > 0" class="font-mono">{{ result.httpStatus }} · </span>
      {{ $t(`checkReason.${result.reason}`) }}
    </td>
  </tr>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { CheckResult } from '@/api/api.types'
import { formatCheckLatency } from '@/utils/status'
import { formatStamp } from '@/utils/time'

import StatusBadge from './StatusBadge.vue'

export default {
  name: 'ChecksTableRow',
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
