<template>
  <dialog ref="dialog" class="m-auto w-full max-w-sm rounded border p-4" @close="$emit('close')">
    <div class="space-y-3">
      <h2 class="text-lg font-semibold">Borrar monitor</h2>
      <p class="text-sm">
        ¿Seguro que querés borrar <strong>{{ monitor?.name }}</strong
        >? Se borra también todo su historial.
      </p>
      <p v-if="error" role="alert" class="text-sm text-red-600">
        {{ $t(error.key, error.params) }}
      </p>
      <div class="flex justify-end gap-2">
        <button type="button" class="rounded border px-3 py-1" @click="$emit('close')">
          Cancelar
        </button>
        <button
          type="button"
          class="rounded border border-red-600 px-3 py-1 text-red-700"
          :disabled="deleting"
          @click="confirm"
        >
          {{ deleting ? 'Borrando…' : 'Borrar' }}
        </button>
      </div>
    </div>
  </dialog>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { mapActions } from 'pinia'

import type { Monitor } from '@/api/api.types'
import { describeError } from '@/api/errors'
import { useMonitorsStore } from '@/stores/monitors'

import type { DeleteMonitorData } from '../components.types'

export default {
  name: 'DeleteMonitorDialog',
  props: {
    open: {
      type: Boolean,
      required: true,
    },
    monitor: {
      type: Object as PropType<Monitor | null>,
      default: null,
    },
  },
  emits: ['close', 'deleted'],
  data(): DeleteMonitorData {
    return {
      deleting: false,
      error: null,
    }
  },
  watch: {
    open(isOpen: boolean) {
      const dialog = this.$refs.dialog
      if (!(dialog instanceof HTMLDialogElement)) {
        return
      }
      if (isOpen) {
        this.deleting = false
        this.error = null
        dialog.showModal()
      } else if (dialog.open) {
        dialog.close()
      }
    },
  },
  methods: {
    ...mapActions(useMonitorsStore, ['remove']),
    async confirm() {
      if (!this.monitor) {
        return
      }
      this.error = null
      this.deleting = true
      try {
        await this.remove(this.monitor.id)
        this.$emit('deleted', this.monitor)
      } catch (error) {
        this.error = describeError(error)
      } finally {
        this.deleting = false
      }
    },
  },
}
</script>
