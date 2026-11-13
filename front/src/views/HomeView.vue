<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-2">
      <h1 class="text-xl font-semibold">{{ $t('home.title') }}</h1>
      <button
        type="button"
        class="rounded border px-3 py-1"
        :disabled="!canCreate"
        @click="openCreate"
      >
        Agregar monitor
      </button>
    </header>

    <p v-if="isGuest && limitReached" class="text-sm text-gray-600">
      Ya tenés tu monitor de prueba. Si querés probar con otra URL, borralo y creá uno nuevo.
    </p>

    <InfoNotice v-if="isGuest" title="Esto es una demo">
      <p>
        Projects Monitor es una app personal. Como invitado podés ver los monitores públicos y crear
        uno propio, que se borra a las 12 horas.
      </p>
    </InfoNotice>

    <div v-if="isAdmin" role="tablist" class="flex gap-2 border-b">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        role="tab"
        class="-mb-px border-b-2 px-3 py-1 text-sm"
        :class="activeTab === tab.id ? 'border-black font-semibold' : 'border-transparent'"
        :aria-selected="activeTab === tab.id"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <InfoNotice v-if="isAdmin && activeTab === 'guests'" title="Pendiente del back">
      <p>
        Todavía no se pueden ver los monitores de los invitados: el back tiene que exponerlos para
        el admin.
      </p>
    </InfoNotice>
    <template v-else>
      <p v-if="loading" class="text-sm">{{ $t('common.loading') }}</p>
      <div v-else-if="error" role="alert" class="space-y-2 text-sm text-red-600">
        <p>{{ $t(error.key, error.params) }}</p>
        <button type="button" class="rounded border px-2 py-1 text-black" @click="fetchAll">
          {{ $t('common.retry') }}
        </button>
      </div>
      <p v-else-if="!items.length" class="text-sm text-gray-600">Todavía no hay monitores.</p>
      <MonitorList v-else :monitors="items" @edit="openEdit" @delete="openDelete" />
    </template>

    <MonitorFormDialog
      :open="formOpen"
      :monitor="editing"
      @close="formOpen = false"
      @saved="onSaved"
    />
    <DeleteMonitorDialog
      :open="deleteOpen"
      :monitor="deleteTarget"
      @close="deleteOpen = false"
      @deleted="deleteOpen = false"
    />
  </section>
</template>

<script lang="ts">
import { mapActions, mapState } from 'pinia'

import type { Monitor } from '@/api/api.types'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import MonitorList from '@/components/monitor/MonitorList.vue'
import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'

import type { HomeData, HomeTab } from './views.types'

const DEFAULT_POLL_SECONDS = 600
const FIRST_CHECK_DELAY_MS = 70_000

export default {
  name: 'HomeView',
  components: { DeleteMonitorDialog, InfoNotice, MonitorFormDialog, MonitorList },
  data(): HomeData {
    return {
      activeTab: 'mine',
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
    ...mapState(useMonitorsStore, ['items', 'loading', 'error', 'canCreate']),
    tabs(): { id: HomeTab; label: string }[] {
      return [
        { id: 'mine', label: 'Mis monitores' },
        { id: 'guests', label: 'Invitados' },
      ]
    },
    limitReached(): boolean {
      return this.limits !== null && this.limits.maxMonitors !== null && !this.canCreate
    },
  },
  mounted() {
    void this.fetchAll()
    const seconds = this.limits?.checkIntervalSeconds ?? DEFAULT_POLL_SECONDS
    this.pollTimer = setInterval(() => this.poll(), seconds * 1_000)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
  },
  beforeUnmount() {
    clearInterval(this.pollTimer)
    clearTimeout(this.firstCheckTimer)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['fetchAll']),
    poll() {
      if (!document.hidden) {
        void this.fetchAll()
      }
    },
    onVisibilityChange() {
      if (!document.hidden) {
        void this.fetchAll()
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
    onSaved() {
      const created = this.editing === null
      this.formOpen = false
      if (created) {
        clearTimeout(this.firstCheckTimer)
        this.firstCheckTimer = setTimeout(() => void this.fetchAll(), FIRST_CHECK_DELAY_MS)
      }
    },
  },
}
</script>
