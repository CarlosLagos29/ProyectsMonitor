<template>
  <section class="space-y-6">
    <RouterLink
      :to="{ name: 'home' }"
      class="inline-flex min-h-11 items-center gap-2 font-cam text-[22px] text-primary uppercase"
    >
      <ArrowLeft :size="18" aria-hidden="true" />
      Volver a las cámaras
    </RouterLink>

    <p v-if="loading && !monitor" class="font-cam text-[22px] text-muted-foreground uppercase">
      Conectando…
    </p>
    <div v-else-if="notFound" class="space-y-4">
      <h1 class="text-[26px] font-bold sm:text-[34px]">Cámara no encontrada</h1>
      <p class="text-muted-foreground">Este monitor no existe o ya venció.</p>
    </div>
    <div v-else-if="error && !monitor" class="space-y-3">
      <InfoNotice tag="Error" tone="destructive" role="alert">
        {{ $t(error.key, error.params) }}
      </InfoNotice>
      <button type="button" class="btn btn-secondary" @click="load">Reintentar</button>
    </div>
    <div v-else-if="monitor" class="space-y-6">
      <header class="space-y-1">
        <p class="cam-label text-muted-foreground">{{ camLabel }}</p>
        <h1 class="text-[28px] leading-tight font-bold break-words sm:text-[36px]">
          {{ monitor.name }}
        </h1>
      </header>

      <div class="grid gap-5 lg:grid-cols-[minmax(0,24rem)_minmax(0,1fr)]">
        <MonitorCard
          :monitor="monitor"
          :cam="currentCam"
          :show-feed="false"
          @edit="formOpen = true"
          @delete="deleteOpen = true"
          @checked="loadSeries"
        />

        <div class="flex min-w-0 flex-col gap-3">
          <CameraFeed
            class="h-50 flex-1 sm:min-h-75"
            :noise="chartNoise"
            :rec="series?.results.length ? 'on' : 'off'"
            label="REC · Latencia 24 H"
            :stamp="series ? checksLabel : ''"
            bottom-left="−24 H"
            bottom-right="Ahora"
            :message="chartMessage"
          >
            <div v-if="series && series.results.length" class="absolute inset-x-3 top-10 bottom-10">
              <LatencyChart :results="series.results" :from="series.from" :to="series.to" />
            </div>
          </CameraFeed>
          <ul class="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <li class="flex items-center gap-1.5">
              <span class="h-0.5 w-4 rounded bg-status-up" aria-hidden="true" />
              Operativo
            </li>
            <li class="flex items-center gap-1.5">
              <span class="h-3 w-0.5 bg-status-blocked/70" aria-hidden="true" />
              Bloqueado
            </li>
            <li class="flex items-center gap-1.5">
              <span class="h-3 w-3 bg-status-down/30" aria-hidden="true" />
              Caído
            </li>
            <li v-if="hasRejected" class="flex items-center gap-1.5">
              <span class="h-3 w-0.5 bg-status-rejected/70" aria-hidden="true" />
              Rechazado
            </li>
          </ul>
        </div>
      </div>

      <section class="space-y-3" aria-labelledby="checks-title">
        <h2 id="checks-title" class="cam-label text-[24px]">Chequeos recientes</h2>
        <InfoNotice v-if="seriesError" tag="Error" tone="destructive" role="alert">
          No pudimos cargar los chequeos. {{ $t(seriesError.key, seriesError.params) }}
          <button type="button" class="ml-2 underline" @click="loadSeries">Reintentar</button>
        </InfoNotice>
        <p v-else-if="seriesLoading && !series" class="text-sm text-muted-foreground">
          Cargando chequeos…
        </p>
        <p v-else-if="!newestFirst.length" class="text-sm text-muted-foreground">
          Todavía no hay chequeos en las últimas 24 h.
        </p>
        <ChecksTable v-else :results="newestFirst" />
      </section>
    </div>

    <MonitorFormDialog
      :open="formOpen"
      :monitor="monitor"
      :cam="currentCam"
      @close="formOpen = false"
      @saved="formOpen = false"
    />
    <DeleteMonitorDialog
      :open="deleteOpen"
      :monitor="monitor"
      @close="deleteOpen = false"
      @deleted="onDeleted"
    />
  </section>
</template>

<script lang="ts">
import { ArrowLeft } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import type { CheckResult, Monitor } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import { getResults } from '@/api/monitors'
import CameraFeed from '@/components/layout/CameraFeed.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import ChecksTable from '@/components/monitor/ChecksTable.vue'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import LatencyChart from '@/components/monitor/LatencyChart.vue'
import MonitorCard from '@/components/monitor/MonitorCard.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'

import type { MonitorDetailData } from './views.types'

const DEFAULT_POLL_SECONDS = 600
const NOISE_DOWN = 0.32
const NOISE_EMPTY = 0.14

export default {
  name: 'MonitorDetailView',
  components: {
    ArrowLeft,
    CameraFeed,
    ChecksTable,
    DeleteMonitorDialog,
    InfoNotice,
    LatencyChart,
    MonitorCard,
    MonitorFormDialog,
  },
  props: {
    id: {
      type: String,
      required: true,
    },
  },
  data(): MonitorDetailData {
    return {
      loading: true,
      notFound: false,
      error: null,
      formOpen: false,
      deleteOpen: false,
      series: null,
      seriesLoading: false,
      seriesError: null,
      pollTimer: undefined,
    }
  },
  computed: {
    ...mapState(useMonitorsStore, ['items', 'currentCam']),
    ...mapState(useSessionStore, ['limits']),
    camLabel(): string {
      return this.currentCam === null ? 'CAM ??' : `CAM ${String(this.currentCam).padStart(2, '0')}`
    },
    monitor(): Monitor | null {
      return this.items.find((item) => item.id === this.id) ?? null
    },
    newestFirst(): CheckResult[] {
      return this.series ? [...this.series.results].reverse() : []
    },
    checksLabel(): string {
      const count = this.series?.results.length ?? 0
      return `${count} ${count === 1 ? 'chequeo' : 'chequeos'}`
    },
    hasRejected(): boolean {
      return this.series?.results.some((result) => result.status === 'REJECTED') ?? false
    },
    chartMessage(): string {
      if (this.monitor?.lastCheck?.status === 'DOWN') {
        return 'Señal perdida'
      }
      if (this.series && !this.series.results.length) {
        return 'Sin chequeos todavía'
      }
      if (!this.series && this.seriesLoading) {
        return 'Conectando…'
      }
      return ''
    },
    chartNoise(): number {
      if (this.monitor?.lastCheck?.status === 'DOWN') {
        return NOISE_DOWN
      }
      return this.series?.results.length ? 0 : NOISE_EMPTY
    },
  },
  watch: {
    id() {
      this.series = null
      void this.load()
    },
  },
  mounted() {
    void this.load()
    const seconds = this.limits?.checkIntervalSeconds ?? DEFAULT_POLL_SECONDS
    this.pollTimer = setInterval(() => this.refresh(), seconds * 1_000)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
  },
  beforeUnmount() {
    clearInterval(this.pollTimer)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
  },
  methods: {
    ...mapActions(useMonitorsStore, ['fetchOne']),
    async load() {
      this.loading = true
      this.notFound = false
      this.error = null
      try {
        await this.fetchOne(this.id)
        void this.loadSeries()
      } catch (error) {
        const apiError = toApiError(error)
        if (apiError.reason === 'NOT_FOUND') {
          this.notFound = true
        } else {
          this.error = describeError(apiError)
        }
      } finally {
        this.loading = false
      }
    },
    async loadSeries() {
      const id = this.id
      this.seriesLoading = true
      this.seriesError = null
      try {
        const series = await getResults(id)
        if (id === this.id) {
          this.series = series
        }
      } catch (error) {
        if (id === this.id) {
          this.seriesError = describeError(error)
        }
      } finally {
        this.seriesLoading = false
      }
    },
    refresh() {
      if (document.hidden || this.notFound) {
        return
      }
      this.fetchOne(this.id)
        .then(() => this.loadSeries())
        .catch(() => undefined)
    },
    onVisibilityChange() {
      if (!document.hidden) {
        this.refresh()
      }
    },
    onDeleted() {
      this.deleteOpen = false
      void this.$router.push({ name: 'home' })
    },
  },
}
</script>
