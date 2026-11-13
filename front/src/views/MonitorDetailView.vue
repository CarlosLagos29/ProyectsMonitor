<template>
  <section class="space-y-4">
    <RouterLink :to="{ name: 'home' }" class="text-sm underline"
      >← Volver a los monitores</RouterLink
    >

    <p v-if="loading && !monitor" class="text-sm">{{ $t('common.loading') }}</p>
    <div v-else-if="notFound" class="space-y-2">
      <h1 class="text-xl font-semibold">{{ $t('monitorDetail.title') }}</h1>
      <p class="text-sm">{{ $t('errors.NOT_FOUND') }}</p>
    </div>
    <div v-else-if="error && !monitor" role="alert" class="space-y-2 text-sm text-red-600">
      <p>{{ $t(error.key, error.params) }}</p>
      <button type="button" class="rounded border px-2 py-1 text-black" @click="load">
        {{ $t('common.retry') }}
      </button>
    </div>
    <template v-else-if="monitor">
      <h1 class="text-xl font-semibold">{{ $t('monitorDetail.title') }}</h1>
      <MonitorCard :monitor="monitor" @edit="formOpen = true" @delete="deleteOpen = true" />
      <dl class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt class="text-gray-600">Próximo chequeo</dt>
        <dd>{{ monitor.nextCheckAt.toLocaleString() }}</dd>
        <dt class="text-gray-600">Creado</dt>
        <dd>{{ monitor.createdAt.toLocaleString() }}</dd>
      </dl>
      <p class="text-sm text-gray-600">
        Próximamente: estadísticas y gráfico de latencia de las últimas 24 h.
      </p>
    </template>

    <MonitorFormDialog
      :open="formOpen"
      :monitor="monitor"
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
import { mapActions, mapState } from 'pinia'

import type { Monitor } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import DeleteMonitorDialog from '@/components/monitor/DeleteMonitorDialog.vue'
import MonitorCard from '@/components/monitor/MonitorCard.vue'
import MonitorFormDialog from '@/components/monitor/MonitorFormDialog.vue'
import { useMonitorsStore } from '@/stores/monitors'

import type { MonitorDetailData } from './views.types'

export default {
  name: 'MonitorDetailView',
  components: { DeleteMonitorDialog, MonitorCard, MonitorFormDialog },
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
    ...mapState(useMonitorsStore, ['items']),
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
