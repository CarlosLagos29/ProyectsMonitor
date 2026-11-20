<template>
  <div class="space-y-3">
    <div class="overflow-x-auto rounded-sm border border-border">
      <table class="w-full min-w-155 text-sm">
        <caption class="sr-only">
          {{
            $t('monitorDetail.table.caption')
          }}
        </caption>
        <thead class="bg-card text-left text-xs text-muted-foreground">
          <tr>
            <th scope="col" class="px-4 py-3 font-medium">{{ $t('monitorDetail.table.date') }}</th>
            <th scope="col" class="px-4 py-3 font-medium">
              {{ $t('monitorDetail.table.status') }}
            </th>
            <th scope="col" class="px-4 py-3 text-right font-medium">
              {{ $t('monitorDetail.table.latency') }}
            </th>
            <th scope="col" class="px-4 py-3 font-medium">
              {{ $t('monitorDetail.table.reason') }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="result in visibleResults"
            :key="result.checkedAt.getTime()"
            class="border-t border-border odd:bg-background even:bg-muted"
          >
            <td class="px-4 py-2 font-mono whitespace-nowrap tabular-nums">
              {{ formatStamp(result.checkedAt) }}
            </td>
            <td class="px-4 py-2">
              <StatusBadge :status="result.status" small />
            </td>
            <td class="px-4 py-2 text-right font-mono whitespace-nowrap tabular-nums">
              {{ latencyText(result) }}
            </td>
            <td class="px-4 py-2">
              <span v-if="result.httpStatus > 0" class="font-mono">{{ result.httpStatus }} · </span>
              {{ $t(`checkReason.${result.reason}`) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <button v-if="hasMore" type="button" class="btn btn-secondary" @click="showMore">
      {{ $t('monitorDetail.table.more') }}
    </button>
  </div>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { CheckResult } from '@/api/api.types'
import { formatStamp } from '@/utils/time'

import type { ChecksTableData } from '../components.types'
import StatusBadge from './StatusBadge.vue'

const PAGE_ROWS = 20

export default {
  name: 'ChecksTable',
  components: { StatusBadge },
  props: {
    results: {
      type: Array as PropType<CheckResult[]>,
      required: true,
    },
  },
  data(): ChecksTableData {
    return {
      visibleRows: PAGE_ROWS,
    }
  },
  computed: {
    visibleResults(): CheckResult[] {
      return this.results.slice(0, this.visibleRows)
    },
    hasMore(): boolean {
      return this.results.length > this.visibleRows
    },
  },
  methods: {
    formatStamp,
    showMore() {
      this.visibleRows += PAGE_ROWS
    },
    latencyText(result: CheckResult): string {
      return result.status === 'REJECTED' ? '—' : `${result.latencyMs} ms`
    },
  },
}
</script>
