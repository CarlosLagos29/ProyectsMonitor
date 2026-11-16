<template>
  <section class="space-y-4">
    <header class="flex flex-wrap items-center justify-between gap-2">
      <h1 class="text-xl font-semibold">{{ $t('home.title') }}</h1>
      <button
        v-if="activeScope === 'OWN'"
        type="button"
        class="rounded border px-3 py-1"
        :disabled="loading || !canCreate"
        @click="openCreate"
      >
        Agregar monitor
      </button>
    </header>

    <p v-if="isGuest && activeScope === 'OWN' && limitReached" class="text-sm text-gray-600">
      Ya tenés tu monitor de prueba. Si querés probar con otra URL, borralo y creá uno nuevo.
    </p>

    <InfoNotice v-if="isGuest" title="Esto es una demo">
      <p>
        Projects Monitor es una app personal. Como invitado podés ver los monitores públicos y crear
        uno propio, que se borra a las 12 horas.
      </p>
    </InfoNotice>

    <div role="tablist" class="flex gap-2 border-b">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        role="tab"
        class="-mb-px border-b-2 px-3 py-1 text-sm"
        :class="activeScope === tab.id ? 'border-black font-semibold' : 'border-transparent'"
        :aria-selected="activeScope === tab.id"
        @click="setScope(tab.id)"
      >
        {{ tab.label }}
      </button>
    </div>

    <p v-if="activeScope === 'GUESTS'" class="text-sm text-gray-600">
      Monitores de invitados que no vencieron. Si pausás uno, el invitado lo puede reanudar: para
      cortarlo del todo, borralo.
    </p>
    <p v-if="loading" class="text-sm">{{ $t('common.loading') }}</p>
    <div v-else-if="error" role="alert" class="space-y-2 text-sm text-red-600">
      <p>{{ $t(error.key, error.params) }}</p>
      <button type="button" class="rounded border px-2 py-1 text-black" @click="fetchPage()">
        {{ $t('common.retry') }}
      </button>
    </div>
    <p v-else-if="!items.length" class="text-sm text-gray-600">{{ emptyText }}</p>
    <template v-else>
      <MonitorList :monitors="items" @edit="openEdit" @delete="openDelete" />
      <nav
        v-if="totalPages > 1"
        aria-label="Páginas de monitores"
        class="flex items-center justify-center gap-3 text-sm"
      >
        <button
          type="button"
          class="rounded border px-3 py-1 disabled:opacity-50"
          :disabled="fetching || currentPage === 0"
          @click="fetchPage(currentPage - 1)"
        >
          Anterior
        </button>
        <span>Página {{ currentPage + 1 }} de {{ totalPages }}</span>
        <button
          type="button"
          class="rounded border px-3 py-1 disabled:opacity-50"
          :disabled="fetching || currentPage >= totalPages - 1"
          @click="fetchPage(currentPage + 1)"
        >
          Siguiente
        </button>
      </nav>
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

import type { Monitor, MonitorScope } from '@/api/api.types'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import MonitorList from '@/components/monitor/MonitorList.vue'
import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'

import type { HomeData } from './views.types'

const DEFAULT_POLL_SECONDS = 600
const FIRST_CHECK_DELAY_MS = 70_000

export default {
  name: 'HomeView',
  components: { DeleteMonitorDialog, InfoNotice, MonitorFormDialog, MonitorList },
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
      'activeScope',
      'loading',
      'fetching',
      'error',
      'canCreate',
      'currentPage',
      'totalPages',
    ]),
    tabs(): { id: MonitorScope; label: string }[] {
      if (this.isGuest) {
        return [
          { id: 'PUBLIC', label: 'Públicos' },
          { id: 'OWN', label: 'Mi monitor' },
        ]
      }
      return [
        { id: 'OWN', label: 'Mis monitores' },
        { id: 'GUESTS', label: 'Invitados' },
      ]
    },
    emptyText(): string {
      if (this.activeScope === 'PUBLIC') {
        return 'Todavía no hay monitores públicos.'
      }
      if (this.activeScope === 'GUESTS') {
        return 'No hay monitores de invitados vigentes.'
      }
      return this.isGuest
        ? 'Todavía no creaste tu monitor. Agregá una URL para probar cómo funciona.'
        : 'Todavía no hay monitores.'
    },
    limitReached(): boolean {
      return this.limits !== null && this.limits.maxMonitors !== null && !this.canCreate
    },
  },
  mounted() {
    void this.fetchPage()
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
    ...mapActions(useMonitorsStore, ['fetchPage', 'setScope']),
    poll() {
      if (!document.hidden) {
        void this.fetchPage()
      }
    },
    onVisibilityChange() {
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
