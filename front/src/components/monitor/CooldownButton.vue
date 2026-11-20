<template>
  <button
    type="button"
    class="btn"
    :class="waiting ? 'btn-secondary' : 'btn-primary'"
    :disabled="checking || waiting"
    @click="$emit('check')"
  >
    <RotateCw :size="18" aria-hidden="true" :class="{ 'animate-spin': checking }" />
    <span aria-live="polite">{{ text }}</span>
  </button>
</template>

<script lang="ts">
import { RotateCw } from 'lucide-vue-next'

export default {
  name: 'CooldownButton',
  components: { RotateCw },
  props: {
    checking: {
      type: Boolean,
      default: false,
    },
    cooldownSeconds: {
      type: Number,
      default: 0,
    },
  },
  emits: ['check'],
  computed: {
    waiting(): boolean {
      return this.cooldownSeconds > 0
    },
    text(): string {
      if (this.checking) {
        return this.$t('monitor.check.running')
      }
      if (this.waiting) {
        return this.$t('monitor.check.retryIn', { seconds: this.cooldownSeconds })
      }
      return this.$t('monitor.check.now')
    },
  },
}
</script>
