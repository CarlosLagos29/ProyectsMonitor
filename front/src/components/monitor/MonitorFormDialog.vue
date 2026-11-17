<template>
  <dialog
    ref="dialog"
    class="dialog"
    aria-labelledby="monitor-form-title"
    @close="$emit('close')"
    @pointerdown.self="pressedBackdrop = true"
    @click.self="onBackdropClick"
  >
    <form class="flex h-full flex-col" novalidate @submit.prevent="submit">
      <header class="flex items-center justify-between gap-3 border-b border-border px-5 py-3">
        <h2 id="monitor-form-title" class="dialog-title">{{ title }}</h2>
        <button
          type="button"
          class="w-4.5 btn btn-secondary btn-icon border-transparent"
          aria-label="Cerrar"
          @click="close"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </header>

      <div class="flex-1 space-y-4 px-5 py-4">
        <div class="space-y-1">
          <label for="monitor-name" class="text-sm font-medium">Nombre</label>
          <input
            id="monitor-name"
            v-model="name"
            type="text"
            maxlength="60"
            class="field"
            :aria-invalid="Boolean(nameError)"
            :aria-describedby="nameError ? 'monitor-name-error' : undefined"
          />
          <p v-if="nameError" id="monitor-name-error" class="field-error">
            <CircleAlert :size="14" aria-hidden="true" />
            {{ nameError }}
          </p>
        </div>

        <div class="space-y-1">
          <label for="monitor-url" class="text-sm font-medium">URL</label>
          <div class="relative">
            <input
              id="monitor-url"
              v-model="url"
              type="url"
              maxlength="2048"
              placeholder="https://ejemplo.com"
              class="field font-mono text-sm"
              :class="{ 'pr-10': isEdit }"
              :disabled="isEdit"
              :aria-invalid="Boolean(urlError)"
              :aria-describedby="urlDescribedBy"
            />
            <Lock
              v-if="isEdit"
              :size="16"
              class="absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground"
              aria-hidden="true"
            />
          </div>
          <p v-if="isEdit" id="monitor-url-help" class="text-xs text-muted-foreground">
            La URL no se puede cambiar. Para otra URL, creá una cámara nueva.
          </p>
          <p v-if="urlError" id="monitor-url-error" class="field-error">
            <CircleAlert :size="14" aria-hidden="true" />
            {{ urlError }}
          </p>
        </div>

        <ToggleSwitch
          v-if="isEdit"
          id="monitor-enabled"
          v-model="enabled"
          label="Activo"
          help="Se chequea cada 10 minutos."
        />
        <ToggleSwitch
          v-if="showPublic"
          id="monitor-public"
          v-model="isPublic"
          label="Público"
          help="Lo ven todos los invitados."
        />

        <InfoNotice v-if="error" tag="Error" tone="destructive" role="alert">
          {{ $t(error.key, error.params) }}
        </InfoNotice>
      </div>

      <footer class="flex justify-end gap-2 border-t border-border px-5 py-3">
        <button type="button" class="btn btn-secondary" @click="close">Cancelar</button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ submitText }}
        </button>
      </footer>
    </form>
  </dialog>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { CircleAlert, Lock, X } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import type { Monitor, MonitorChanges } from '@/api/api.types'
import { describeError, toApiError } from '@/api/errors'
import ToggleSwitch from '@/components/form/ToggleSwitch.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import { useMonitorsStore } from '@/stores/monitors'

import type { MonitorFormData } from '../components.types'

const NAME_MAX = 60

export default {
  name: 'MonitorFormDialog',
  components: { CircleAlert, InfoNotice, Lock, ToggleSwitch, X },
  props: {
    open: {
      type: Boolean,
      required: true,
    },
    monitor: {
      type: Object as PropType<Monitor | null>,
      default: null,
    },
    cam: {
      type: Number as PropType<number | null>,
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
      pressedBackdrop: false,
    }
  },
  computed: {
    ...mapState(useMonitorsStore, ['canSetPublic']),
    isEdit(): boolean {
      return this.monitor !== null
    },
    title(): string {
      if (!this.isEdit) {
        return 'Nueva cámara'
      }
      return this.cam === null ? 'Editar cámara' : `Editar CAM ${String(this.cam).padStart(2, '0')}`
    },
    submitText(): string {
      if (this.saving) {
        return 'Guardando…'
      }
      return this.isEdit ? 'Guardar' : 'Instalar cámara'
    },
    urlDescribedBy(): string | undefined {
      const ids = [this.isEdit ? 'monitor-url-help' : '', this.urlError ? 'monitor-url-error' : '']
      return ids.filter(Boolean).join(' ') || undefined
    },
    showPublic(): boolean {
      return this.canSetPublic && !this.monitor?.expiresAt
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
        return 'Completá la URL: tiene que empezar con http:// o https://.'
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
    onBackdropClick() {
      if (this.pressedBackdrop && !this.saving) {
        this.close()
      }
      this.pressedBackdrop = false
    },
    changes(monitor: Monitor): MonitorChanges {
      const changes: MonitorChanges = {}
      if (this.name.trim() !== monitor.name) {
        changes.name = this.name
      }
      if (this.enabled !== monitor.enabled) {
        changes.enabled = this.enabled
      }
      if (this.showPublic && this.isPublic !== monitor.isPublic) {
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
            this.fieldErrors = {
              ...this.fieldErrors,
              url: 'Revisá la URL: tiene que empezar con http:// o https://.',
            }
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
