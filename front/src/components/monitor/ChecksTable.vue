<template>
  <div class="space-y-3">
    <ul
      class="divide-y divide-border rounded-sm border border-border text-sm sm:hidden"
      :aria-label="$t('monitorDetail.table.caption')"
    >
      <ChecksListItem
        v-for="result in visibleResults"
        :key="result.checkedAt.getTime()"
        :result="result"
      />
    </ul>
    <div class="hidden overflow-x-auto rounded-sm border border-border sm:block">
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
          <ChecksTableRow
            v-for="result in visibleResults"
            :key="result.checkedAt.getTime()"
            :result="result"
          />
        </tbody>
      </table>
    </div>
    <button
      v-if="hasMore"
      type="button"
      class="btn btn-secondary max-sm:min-h-9 max-sm:w-full max-sm:px-3 max-sm:text-[17px]"
      @click="showMore"
    >
      {{ $t('monitorDetail.table.more') }}
    </button>
  </div>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import type { CheckResult } from '@/api/api.types'

import type { ChecksTableData } from '../components.types'
import ChecksListItem from './ChecksListItem.vue'
import ChecksTableRow from './ChecksTableRow.vue'

const PAGE_ROWS = 20

export default {
  name: 'ChecksTable',
  components: { ChecksListItem, ChecksTableRow },
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
    showMore() {
      this.visibleRows += PAGE_ROWS
    },
  },
}
</script>
