<template>
  <button
    type="button"
    class="btn btn-secondary btn-icon max-sm:size-9 max-sm:min-h-9"
    :aria-label="enabled ? $t('header.soundOff') : $t('header.soundOn')"
    :aria-pressed="enabled"
    :title="enabled ? $t('header.soundOff') : $t('header.soundOn')"
    @click="toggle"
  >
    <Volume2 v-if="enabled" :size="16" aria-hidden="true" />
    <VolumeX v-else :size="16" aria-hidden="true" />
  </button>
</template>

<script lang="ts">
import { markRaw } from 'vue'
import { Volume2, VolumeX } from 'lucide-vue-next'

import type { AmbienceToggleData } from '../components.types'

const SOURCE = '/vhs-static.mp3'
const STORAGE_KEY = 'ambience'
const VOLUME = 1
const UNLOCK_EVENTS = ['pointerdown', 'keydown'] as const

function readEnabled(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) !== 'off'
  } catch {
    return true
  }
}

export default {
  name: 'AmbienceToggle',
  components: { Volume2, VolumeX },
  data(): AmbienceToggleData {
    return {
      enabled: readEnabled(),
      context: null,
    }
  },
  mounted() {
    UNLOCK_EVENTS.forEach((event) => window.addEventListener(event, this.unlock))
  },
  beforeUnmount() {
    UNLOCK_EVENTS.forEach((event) => window.removeEventListener(event, this.unlock))
    this.context?.close().catch(() => undefined)
  },
  methods: {
    async unlock() {
      UNLOCK_EVENTS.forEach((event) => window.removeEventListener(event, this.unlock))
      const context = markRaw(new AudioContext())
      this.context = context
      if (!this.enabled) {
        await context.suspend()
      }
      try {
        const response = await fetch(SOURCE)
        const buffer = await context.decodeAudioData(await response.arrayBuffer())
        const gain = context.createGain()
        gain.gain.value = VOLUME
        const source = context.createBufferSource()
        source.buffer = buffer
        source.loop = true
        source.connect(gain).connect(context.destination)
        source.start()
      } catch {
        this.context = null
      }
    },
    toggle() {
      this.enabled = !this.enabled
      try {
        localStorage.setItem(STORAGE_KEY, this.enabled ? 'on' : 'off')
      } catch {}
      if (this.enabled) {
        this.context?.resume().catch(() => undefined)
      } else {
        this.context?.suspend().catch(() => undefined)
      }
    },
  },
}
</script>
