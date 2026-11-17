<template>
  <section class="space-y-6">
    <InfoNotice v-if="isGuest" tag="Aviso" role="note">
      <strong>Estás en modo demo.</strong> Projects Monitor es una app personal: lo que crees como
      invitado es temporal y se borra solo a las 12 h.
    </InfoNotice>

    <header class="flex flex-wrap items-end justify-between gap-4">
      <div class="min-w-0 space-y-1">
        <h1 class="text-[26px] leading-tight font-bold sm:text-[34px]">{{ heading.title }}</h1>
        <p class="text-muted-foreground">{{ heading.subtitle }}</p>
      </div>
      <button
        v-if="isAdmin && activeScope === 'OWN'"
        type="button"
        class="btn btn-primary"
        :disabled="loading || !canCreate"
        @click="openCreate"
      >
        <Plus :size="18" aria-hidden="true" />
        Agregar monitor
      </button>
    </header>

    <div role="tablist" aria-label="Listas de monitores" class="flex flex-wrap gap-2">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        role="tab"
        class="btn text-[22px]"
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
          <CameraFeed class="h-37.5 min-[720px]:h-42.5" :noise="0.18">
            <template #top-left>
              <span class="rec-dot-off" aria-hidden="true" />
              Conectando
            </template>
            <template #message>Conectando…</template>
          </CameraFeed>
          <span class="block h-4 w-2/3 rounded bg-skeleton" />
          <span class="block h-4 w-1/2 rounded bg-skeleton" />
          <span class="mt-auto block h-11 w-full rounded bg-skeleton" />
        </div>
      </div>

      <div v-else-if="error" class="panel flex flex-col gap-4 border-destructive p-4">
        <CameraFeed class="min-h-50 flex-1" :noise="0.32">
          <template #top-left>
            <span class="rec-dot-off" aria-hidden="true" />
            Sin señal
          </template>
          <template #message>Señal perdida</template>
        </CameraFeed>
        <InfoNotice tag="Error" tone="destructive" role="alert">
          No pudimos cargar los monitores. {{ $t(error.key, error.params) }}
        </InfoNotice>
        <button type="button" class="btn btn-secondary self-start" @click="fetchPage()">
          <RotateCw :size="18" aria-hidden="true" />
          Reintentar
        </button>
      </div>

      <CameraFeed v-else-if="!items.length" class="min-h-75" :noise="0.14">
        <template #top-left>
          <span class="rec-dot-off" aria-hidden="true" />
          Cam -- · Libre
        </template>
        <template #message>
          <p class="text-[32px]">No hay cámaras instaladas</p>
          <p class="max-w-sm font-sans text-sm text-muted-foreground normal-case">
            {{ emptyText }}
          </p>
          <button
            v-if="activeScope === 'OWN'"
            type="button"
            class="btn btn-primary mt-3"
            :disabled="!canCreate"
            @click="openCreate"
          >
            <Plus :size="18" aria-hidden="true" />
            Agregar monitor
          </button>
        </template>
      </CameraFeed>

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
          aria-label="Páginas de monitores"
          class="flex flex-wrap items-center justify-center gap-3"
        >
          <button
            type="button"
            class="btn btn-secondary"
            :disabled="fetching || currentPage === 0"
            @click="fetchPage(currentPage - 1)"
          >
            <ChevronLeft :size="18" aria-hidden="true" />
            Anterior
          </button>
          <span class="font-cam text-[22px] uppercase" aria-live="polite">
            Página {{ currentPage + 1 }} de {{ totalPages }}
          </span>
          <button
            type="button"
            class="btn btn-secondary"
            :disabled="fetching || currentPage >= totalPages - 1"
            @click="fetchPage(currentPage + 1)"
          >
            Siguiente
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
      @deleted="deleteOpen = false"
    />
  </section>
</template>

<script lang="ts">
import { ChevronLeft, ChevronRight, Plus, RotateCw } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import type { Monitor, MonitorScope } from '@/api/api.types'
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

const DEFAULT_POLL_SECONDS = 600
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
    tabs(): { id: MonitorScope; label: string }[] {
      if (this.isGuest) {
        return [
          { id: 'PUBLIC', label: 'Públicos' },
          { id: 'OWN', label: 'Mi monitor' },
        ]
      }
      return [
        { id: 'OWN', label: 'Mis monitores' },
        { id: 'GUESTS', label: 'De invitados' },
      ]
    },
    heading(): { title: string; subtitle: string } {
      if (this.activeScope === 'PUBLIC') {
        return {
          title: 'Monitores públicos',
          subtitle: 'Los proyectos que Carlos vigila, en solo lectura.',
        }
      }
      if (this.activeScope === 'GUESTS') {
        return {
          title: 'Monitores de invitados',
          subtitle:
            'Los que crearon los invitados y no vencieron. Si pausás uno, el invitado lo puede reanudar: para cortarlo del todo, borralo.',
        }
      }
      if (this.isGuest) {
        return {
          title: 'Mi monitor',
          subtitle: 'Probá con una URL tuya: se chequea cada 10 minutos.',
        }
      }
      return {
        title: 'Mis monitores',
        subtitle: 'Todos tus monitores, públicos y privados.',
      }
    },
    emptyText(): string {
      if (this.activeScope === 'PUBLIC') {
        return 'Todavía no hay monitores públicos.'
      }
      if (this.activeScope === 'GUESTS') {
        return 'Ningún invitado tiene un monitor vigente.'
      }
      return this.isGuest
        ? 'Agregá una URL para probar cómo funciona. Se borra sola a las 12 h.'
        : 'Agregá el primer proyecto para empezar a vigilarlo.'
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
    this.pollTimer = setInterval(() => this.poll(), seconds * 1_000)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
  },
  beforeUnmount() {
    clearInterval(this.pollTimer)
    clearTimeout(this.firstCheckTimer)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['fetchPage', 'setScope', 'setCurrentCam']),
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
