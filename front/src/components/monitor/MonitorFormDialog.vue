<template>
  <dialog ref="dialog" class="m-auto w-full max-w-md rounded border p-4" @close="$emit('close')">
    <form class="space-y-3" novalidate @submit.prevent="submit">
      <h2 class="text-lg font-semibold">{{ isEdit ? 'Editar monitor' : 'Agregar monitor' }}</h2>

      <label class="block">
        <span class="text-sm">Nombre</span>
        <input
          v-model="name"
          type="text"
          maxlength="60"
          class="mt-1 w-full rounded border px-2 py-1"
          :aria-invalid="Boolean(nameError)"
        />
        <span v-if="nameError" class="text-sm text-red-600">{{ nameError }}</span>
      </label>

      <label class="block">
        <span class="text-sm">URL</span>
        <input
          v-model="url"
          type="url"
          maxlength="2048"
          placeholder="https://ejemplo.com"
          class="mt-1 w-full rounded border px-2 py-1 disabled:bg-gray-100"
          :disabled="isEdit"
          :aria-invalid="Boolean(urlError)"
        />
        <span v-if="isEdit" class="text-sm text-gray-600">
          La URL no se puede cambiar: para vigilar otra, creá un monitor nuevo.
        </span>
        <span v-if="urlError" class="text-sm text-red-600">{{ urlError }}</span>
      </label>

      <label v-if="isEdit" class="flex items-center gap-2 text-sm">
        <input v-model="enabled" type="checkbox" />
        Activo (si lo desactivás, no se chequea solo)
      </label>

      <label v-if="canSetPublic" class="flex items-center gap-2 text-sm">
        <input v-model="isPublic" type="checkbox" />
        Público (lo ven los invitados)
      </label>

      <p v-if="error" role="alert" class="text-sm text-red-600">
        {{ $t(error.key, error.params) }}
      </p>

      <div class="flex justify-end gap-2">
        <button type="button" class="rounded border px-3 py-1" @click="close">Cancelar</button>
        <button type="submit" class="rounded border px-3 py-1" :disabled="saving">
          {{ saving ? 'Guardando…' : 'Guardar' }}
        </button>
      </div>
    </form>
  </dialog>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { mapActions, mapState } from 'pinia'

import type { Monitor, MonitorChanges } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import { useMonitorsStore } from '@/stores/monitors'

import type { MonitorFormData } from '../components.types'

const NAME_MAX = 60

export default {
  name: 'MonitorFormDialog',
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
  emits: ['close', 'saved'],
  data(): MonitorFormData {
    return {
      name: '',
      url: '',
      isPublic: false,
      enabled: true,
      submitted: false,
      saving: false,
      fieldErrors: {},
      error: null,
    }
  },
  computed: {
    ...mapState(useMonitorsStore, ['canSetPublic']),
    isEdit(): boolean {
      return this.monitor !== null
    },
    nameError(): string {
      if (this.fieldErrors.name) {
        return 'Revisá el nombre: tiene que tener entre 1 y 60 caracteres.'
      }
      if (this.submitted && !this.name.trim()) {
        return 'El nombre es obligatorio.'
      }
      return ''
    },
    urlError(): string {
      if (this.fieldErrors.url) {
        return this.fieldErrors.url
      }
      if (this.submitted && !this.isEdit && !this.url.trim()) {
        return 'La URL es obligatoria.'
      }
      return ''
    },
  },
  watch: {
    open(isOpen: boolean) {
      const dialog = this.$refs.dialog
      if (!(dialog instanceof HTMLDialogElement)) {
        return
      }
      if (isOpen) {
        this.reset()
        dialog.showModal()
      } else if (dialog.open) {
        dialog.close()
      }
    },
  },
  methods: {
    ...mapActions(useMonitorsStore, ['create', 'update']),
    reset() {
      this.name = this.monitor?.name ?? ''
      this.url = this.monitor?.url ?? ''
      this.isPublic = this.monitor?.isPublic ?? false
      this.enabled = this.monitor?.enabled ?? true
      this.submitted = false
      this.saving = false
      this.fieldErrors = {}
      this.error = null
    },
    close() {
      this.$emit('close')
    },
    changes(monitor: Monitor): MonitorChanges {
      const changes: MonitorChanges = {}
      if (this.name.trim() !== monitor.name) {
        changes.name = this.name
      }
      if (this.enabled !== monitor.enabled) {
        changes.enabled = this.enabled
      }
      if (this.canSetPublic && this.isPublic !== monitor.isPublic) {
        changes.isPublic = this.isPublic
      }
      return changes
    },
    async submit() {
      this.submitted = true
      this.fieldErrors = {}
      this.error = null
      if (this.nameError || this.urlError || this.name.trim().length > NAME_MAX) {
        return
      }
      this.saving = true
      try {
        const saved = this.monitor
          ? await this.update(this.monitor.id, this.changes(this.monitor))
          : await this.create({ name: this.name, url: this.url, isPublic: this.isPublic })
        this.$emit('saved', saved)
      } catch (error) {
        const apiError = toApiError(error)
        if (apiError.reason === 'INVALID_URL') {
          this.fieldErrors = { url: this.$t('errors.INVALID_URL') }
        } else if (
          apiError.reason === 'VALIDATION_ERROR' &&
          Object.keys(apiError.fieldErrors).length
        ) {
          this.fieldErrors = apiError.fieldErrors
          if (this.fieldErrors.url) {
            this.fieldErrors = { ...this.fieldErrors, url: 'Revisá la URL.' }
          }
        } else {
          this.error = describeError(apiError)
        }
      } finally {
        this.saving = false
      }
    },
  },
}
</script>
