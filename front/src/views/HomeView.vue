<template>
  <section class="space-y-6">
    <InfoNotice v-if="isGuest" :tag="$t('home.noticeTag')" role="note">
      <strong>{{ $t('home.demo') }}</strong> {{ $t('home.demoDetail', { hours: 12 }) }}
    </InfoNotice>

    <header class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="min-w-0 text-[26px] leading-tight font-bold sm:text-[34px]">{{ title }}</h1>
      <button
        v-if="activeScope === 'OWN' && canCreate"
        type="button"
        class="btn btn-primary max-sm:min-h-9 max-sm:px-3 max-sm:text-[17px]"
        :disabled="loading"
        @click="openCreate"
      >
        <Plus :size="18" aria-hidden="true" />
        {{ $t('home.add') }}
      </button>
    </header>

    <div role="tablist" :aria-label="$t('home.tabsLabel')" class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        role="tab"
        class="btn text-[22px] max-sm:min-h-9 max-sm:px-3 max-sm:text-[17px]"
        :class="activeScope === tab.id ? 'btn-primary' : 'btn-secondary'"
        :aria-selected="activeScope === tab.id"
        @click="setScope(tab.id)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="grid min-h-120">
      <div v-if="loading" class="grid gap-5 min-[720px]:grid-cols-2" aria-busy="true">
        <div
          v-for="index in SKELETONS"
          :key="index"
          class="panel flex flex-col gap-4 p-4"
          :class="{ 'max-[719px]:hidden': index > 1 }"
        >
          <div class="flex items-center gap-3">
            <span class="h-5 w-14 rounded bg-skeleton" />
            <span class="h-5 flex-1 rounded bg-skeleton" />
            <span class="h-7 w-24 rounded bg-skeleton" />
          </div>
          <CameraFeed
            class="h-37.5 min-[720px]:h-42.5"
            :noise="NOISE.waiting"
            rec="off"
            :label="$t('common.feed.connecting')"
            :message="$t('common.feed.connectingMessage')"
          />
          <span class="block h-4 w-2/3 rounded bg-skeleton" />
          <span class="block h-4 w-1/2 rounded bg-skeleton" />
          <span class="mt-auto block h-11 w-full rounded bg-skeleton" />
        </div>
      </div>

      <div v-else-if="error" class="panel flex flex-col gap-4 border-destructive p-4">
        <CameraFeed
          class="min-h-50 flex-1"
          :noise="NOISE.down"
          rec="off"
          :label="$t('common.feed.noSignal')"
          :message="$t('common.feed.signalLost')"
        />
        <InfoNotice :tag="$t('common.error')" tone="destructive" role="alert">
          {{ $t('home.loadError') }} {{ $t(error.key, error.params) }}
        </InfoNotice>
        <button type="button" class="btn btn-secondary self-start" @click="fetchPage()">
          <RotateCw :size="18" aria-hidden="true" />
          {{ $t('common.retry') }}
        </button>
      </div>

      <CameraFeed
        v-else-if="!items.length"
        class="min-h-75"
        :noise="NOISE.empty"
        rec="off"
        label="Cam --"
        :message="$t('home.empty')"
        message-class="text-[32px] text-foreground"
        :detail="emptyText"
      />

      <div
        v-else-if="isGuest && activeScope === 'OWN' && ownMonitor"
        class="grid gap-5 min-[720px]:grid-cols-2"
      >
        <MonitorCard :monitor="ownMonitor" :cam="1" @edit="openEdit" @delete="openDelete" />
        <GuestShiftPanel :monitor="ownMonitor" />
      </div>

      <div v-else class="space-y-5">
        <MonitorList :monitors="items" :offset="offset" @edit="openEdit" @delete="openDelete" />
        <nav
          v-if="totalPages > 1"
          :aria-label="$t('home.pagesLabel')"
          class="flex items-center justify-center gap-3"
        >
          <button
            type="button"
            class="btn btn-secondary max-sm:size-9 max-sm:min-h-9 max-sm:px-0"
            :aria-label="$t('home.previous')"
            :disabled="fetching || currentPage === 0"
            @click="fetchPage(currentPage - 1)"
          >
            <ChevronLeft :size="18" aria-hidden="true" />
          </button>
          <span class="font-cam text-[22px] uppercase" aria-live="polite">
            {{ $t('home.page', { current: currentPage + 1, total: totalPages }) }}
          </span>
          <button
            type="button"
            class="btn btn-secondary max-sm:size-9 max-sm:min-h-9 max-sm:px-0"
            :aria-label="$t('home.next')"
            :disabled="fetching || currentPage >= totalPages - 1"
            @click="fetchPage(currentPage + 1)"
          >
            <ChevronRight :size="18" aria-hidden="true" />
          </button>
        </nav>
      </div>
    </div>

    <MonitorFormDialog
      :open="formOpen"
      :monitor="editing"
      :cam="editingCam"
      @close="formOpen = false"
      @saved="onSaved"
    />
    <DeleteMonitorDialog
      :open="deleteOpen"
      :monitor="deleteTarget"
      @close="deleteOpen = false"
      @deleted="onDeleted"
    />
  </section>
</template>

<script lang="ts">
import { ChevronLeft, ChevronRight, Plus, RotateCw } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import type { Monitor, MonitorScope } from '@/api/api.types'
import { DEFAULT_POLL_SECONDS, NOISE } from '@/constants'
import CameraFeed from '@/components/layout/CameraFeed.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import GuestShiftPanel from '@/components/monitor/GuestShiftPanel.vue'
import MonitorCard from '@/components/monitor/MonitorCard.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import MonitorList from '@/components/monitor/MonitorList.vue'
import { PAGE_SIZE, useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'

import type { HomeData } from './views.types'

const FIRST_CHECK_DELAY_MS = 70_000
const SKELETONS = [1, 2]

export default {
  name: 'HomeView',
  components: {
    CameraFeed,
    ChevronLeft,
    ChevronRight,
    DeleteMonitorDialog,
    GuestShiftPanel,
    InfoNotice,
    MonitorCard,
    MonitorFormDialog,
    MonitorList,
    Plus,
    RotateCw,
  },
  data(): HomeData {
    return {
      formOpen: false,
      editing: null,
      deleteOpen: false,
      deleteTarget: null,
      pollTimer: undefined,
      firstCheckTimer: undefined,
    }
  },
  computed: {
    ...mapState(useSessionStore, ['isAdmin', 'isGuest', 'limits']),
    ...mapState(useMonitorsStore, [
      'items',
      'page',
      'activeScope',
      'loading',
      'fetching',
      'error',
      'canCreate',
      'currentPage',
      'totalPages',
    ]),
    SKELETONS: () => SKELETONS,
    NOISE: () => NOISE,
    tabs(): { id: MonitorScope; label: string }[] {
      if (this.isGuest) {
        return [
          { id: 'PUBLIC', label: this.$t('home.tabs.public') },
          { id: 'OWN', label: this.$t('home.tabs.guestOwn') },
        ]
      }
      return [
        { id: 'OWN', label: this.$t('home.tabs.adminOwn') },
        { id: 'GUESTS', label: this.$t('home.tabs.guests') },
      ]
    },
    section(): 'public' | 'guests' | 'guestOwn' | 'adminOwn' {
      if (this.activeScope === 'PUBLIC') {
        return 'public'
      }
      if (this.activeScope === 'GUESTS') {
        return 'guests'
      }
      return this.isGuest ? 'guestOwn' : 'adminOwn'
    },
    title(): string {
      return this.$t(`home.${this.section}.title`)
    },
    emptyText(): string {
      return this.$t(`home.${this.section}.empty`, { hours: 12 })
    },
    ownMonitor(): Monitor | null {
      return this.items[0] ?? null
    },
    offset(): number {
      return this.page ? this.page.number * this.page.size : this.currentPage * PAGE_SIZE
    },
    editingCam(): number | null {
      if (!this.editing) {
        return null
      }
      const index = this.items.findIndex((item) => item.id === this.editing?.id)
      return index === -1 ? null : this.offset + index + 1
    },
  },
  mounted() {
    this.setCurrentCam(null)
    void this.fetchPage()
    const seconds = this.limits?.checkIntervalSeconds ?? DEFAULT_POLL_SECONDS
    this.pollTimer = setInterval(() => this.refresh(), seconds * 1_000)
    document.addEventListener('visibilitychange', this.refresh)
  },
  beforeUnmount() {
    clearInterval(this.pollTimer)
    clearTimeout(this.firstCheckTimer)
    document.removeEventListener('visibilitychange', this.refresh)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['fetchPage', 'setScope', 'setCurrentCam']),
    refresh() {
      if (!document.hidden) {
        void this.fetchPage()
      }
    },
    openCreate() {
      this.editing = null
      this.formOpen = true
    },
    openEdit(monitor: Monitor) {
      this.editing = monitor
      this.formOpen = true
    },
    openDelete(monitor: Monitor) {
      this.deleteTarget = monitor
      this.deleteOpen = true
    },
    onDeleted() {
      this.deleteOpen = false
      void this.fetchPage(0)
    },
    onSaved() {
      const created = this.editing === null
      this.formOpen = false
      if (created) {
        clearTimeout(this.firstCheckTimer)
        this.firstCheckTimer = setTimeout(() => void this.fetchPage(), FIRST_CHECK_DELAY_MS)
      }
    },
  },
}
</script>
