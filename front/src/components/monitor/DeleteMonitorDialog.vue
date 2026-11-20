<template>
  <dialog
    ref="dialog"
    class="dialog border-destructive"
    role="alertdialog"
    aria-labelledby="delete-monitor-title"
    aria-describedby="delete-monitor-text"
    @close="$emit('close')"
    @pointerdown.self="pressedBackdrop = true"
    @click.self="onBackdropClick"
  >
    <div class="flex h-full flex-col">
      <header class="flex items-center justify-between gap-3 border-b border-border px-5 py-3">
        <h2 id="delete-monitor-title" class="dialog-title">{{ $t('monitor.delete.title') }}</h2>
        <button
          type="button"
          class="w-4.5 btn btn-secondary btn-icon border-transparent"
          :aria-label="$t('common.close')"
          @click="$emit('close')"
        >
          <X :size="18" aria-hidden="true" />
        </button>
      </header>

      <div class="flex-1 space-y-4 px-5 py-4">
        <CameraFeed
          class="h-24"
          :noise="0.32"
          :message="$t('monitor.delete.disconnecting')"
          message-class="text-[24px] text-foreground"
        />
        <p id="delete-monitor-text" class="text-sm">
          {{ $t('monitor.delete.text', { name: monitor?.name }) }}
        </p>
        <InfoNotice v-if="isGuestMonitor" :tag="$t('monitor.delete.guestTag')" tone="warning">
          {{ guestText }}
        </InfoNotice>
        <InfoNotice v-if="error" :tag="$t('common.error')" tone="destructive" role="alert">
          {{ $t(error.key, error.params) }}
        </InfoNotice>
      </div>

      <footer class="flex justify-end gap-2 border-t border-border px-5 py-3">
        <button type="button" class="btn btn-secondary" @click="$emit('close')">
          {{ $t('common.cancel') }}
        </button>
        <button type="button" class="btn btn-danger-solid" :disabled="deleting" @click="confirm">
          <Trash2 :size="18" aria-hidden="true" />
          {{ deleting ? $t('monitor.delete.deleting') : $t('common.delete') }}
        </button>
      </footer>
    </div>
  </dialog>
</template>

<script lang="ts">
import type { PropType } from 'vue'
import { Trash2, X } from 'lucide-vue-next'
import { mapActions } from 'pinia'

import type { Monitor } from '@/api/api.types'
import { describeError } from '@/api/errors'
import CameraFeed from '@/components/layout/CameraFeed.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import { useMonitorsStore } from '@/stores/monitors'
import { formatHoursMinutes } from '@/utils/time'

import type { DeleteMonitorData } from '../components.types'

export default {
  name: 'DeleteMonitorDialog',
  components: { CameraFeed, InfoNotice, Trash2, X },
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
      pressedBackdrop: false,
    }
  },
  computed: {
    isGuestMonitor(): boolean {
      return this.monitor !== null && !this.monitor.owned
    },
    guestText(): string {
      const expiresAt = this.monitor?.expiresAt
      if (!expiresAt) {
        return this.$t('monitor.delete.guest')
      }
      return this.$t('monitor.delete.guestRemaining', {
        time: formatHoursMinutes(expiresAt.getTime() - Date.now()),
      })
    },
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
    onBackdropClick() {
      if (this.pressedBackdrop && !this.deleting) {
        this.$emit('close')
      }
      this.pressedBackdrop = false
    },
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
