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
          :aria-label="$t('common.close')"
          @click="close"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </header>

      <div class="flex-1 space-y-4 px-5 py-4">
        <div class="space-y-1">
          <label for="monitor-name" class="mb-1 block text-sm font-medium">{{
            $t('monitor.form.name')
          }}</label>
          <input
            id="monitor-name"
            v-model="name"
            type="text"
            :maxlength="NAME_MAX"
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
          <label for="monitor-url" class="mb-1 block text-sm font-medium">{{
            $t('monitor.form.url')
          }}</label>
          <div class="relative">
            <input
              id="monitor-url"
              v-model="url"
              type="url"
              maxlength="2048"
              :placeholder="$t('monitor.form.urlPlaceholder')"
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
            {{ $t('monitor.form.urlLocked') }}
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
          :label="$t('monitor.form.enabled')"
        />
        <ToggleSwitch
          v-if="showPublic"
          id="monitor-public"
          v-model="isPublic"
          :label="$t('monitor.form.public')"
          :help="$t('monitor.form.publicHelp')"
        />

        <InfoNotice v-if="error" :tag="$t('common.error')" tone="destructive" role="alert">
          {{ $t(error.key, error.params) }}
        </InfoNotice>
      </div>

      <footer class="flex justify-end gap-2 border-t border-border px-5 py-3">
        <button
          type="button"
          class="btn btn-secondary max-sm:min-h-9 max-sm:px-3 max-sm:text-[17px]"
          @click="close"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          type="submit"
          class="btn btn-primary max-sm:min-h-9 max-sm:px-3 max-sm:text-[17px]"
          :disabled="saving"
        >
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
import { formatCam } from '@/utils/cam'

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
    NAME_MAX: () => NAME_MAX,
    isEdit(): boolean {
      return this.monitor !== null
    },
    title(): string {
      if (!this.isEdit) {
        return this.$t('monitor.form.create')
      }
      return this.cam === null
        ? this.$t('monitor.form.edit')
        : this.$t('monitor.form.editCam', { cam: formatCam(this.cam) })
    },
    submitText(): string {
      if (this.saving) {
        return this.$t('monitor.form.saving')
      }
      return this.isEdit ? this.$t('common.save') : this.$t('monitor.form.install')
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
        return this.$t('monitor.form.nameInvalid', { max: NAME_MAX })
      }
      if (this.submitted && !this.name.trim()) {
        return this.$t('monitor.form.nameRequired')
      }
      return ''
    },
    urlError(): string {
      if (this.fieldErrors.url) {
        return this.fieldErrors.url
      }
      if (this.submitted && !this.isEdit && !this.url.trim()) {
        return this.$t('monitor.form.urlRequired')
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
      if (this.nameError || this.urlError) {
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
              url: this.$t('monitor.form.urlInvalid'),
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
