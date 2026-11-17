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
    <template v-else-if="monitor">
      <header class="space-y-1">
        <p class="cam-label text-muted-foreground">{{ camLabel }}</p>
        <h1 class="text-[28px] leading-tight font-bold break-words sm:text-[36px]">
          {{ monitor.name }}
        </h1>
      </header>
      <div class="max-w-2xl">
        <MonitorCard
          :monitor="monitor"
          :cam="currentCam"
          @edit="formOpen = true"
          @delete="deleteOpen = true"
        />
      </div>
      <dl class="grid max-w-2xl grid-cols-2 gap-3">
        <div class="panel p-4">
          <dt class="text-xs text-muted-foreground">Próximo chequeo</dt>
          <dd class="font-mono text-sm">{{ monitor.nextCheckAt.toLocaleString() }}</dd>
        </div>
        <div class="panel p-4">
          <dt class="text-xs text-muted-foreground">Creado</dt>
          <dd class="font-mono text-sm">{{ monitor.createdAt.toLocaleString() }}</dd>
        </div>
      </dl>
      <InfoNotice tag="Próximamente">
        Estadísticas, gráfico de latencia y chequeos de las últimas 24 h.
      </InfoNotice>
    </template>

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

import type { Monitor } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import MonitorCard from '@/components/monitor/MonitorCard.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import { useMonitorsStore } from '@/stores/monitors'

import type { MonitorDetailData } from './views.types'

export default {
  name: 'MonitorDetailView',
  components: { ArrowLeft, DeleteMonitorDialog, InfoNotice, MonitorCard, MonitorFormDialog },
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
    }
  },
  computed: {
    ...mapState(useMonitorsStore, ['items', 'currentCam']),
    camLabel(): string {
      return this.currentCam === null ? 'CAM ??' : `CAM ${String(this.currentCam).padStart(2, '0')}`
    },
    monitor(): Monitor | null {
      return this.items.find((item) => item.id === this.id) ?? null
    },
  },
  watch: {
    id() {
      void this.load()
    },
  },
  mounted() {
    void this.load()
  },
  methods: {
    ...mapActions(useMonitorsStore, ['fetchOne']),
    async load() {
      this.loading = true
      this.notFound = false
      this.error = null
      try {
        await this.fetchOne(this.id)
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
    onDeleted() {
      this.deleteOpen = false
      void this.$router.push({ name: 'home' })
    },
  },
}
</script>
