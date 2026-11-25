<template>
  <dialog
    ref="dialog"
    class="m-0 h-dvh max-h-none w-screen max-w-none border-0 bg-black p-0 backdrop:bg-black"
    :aria-label="$t('intruder.title')"
    @cancel.prevent
    @close="reopen"
  >
    <audio ref="audio" src="/jumpscare/scream.mp3" preload="auto" />
    <div v-if="phase === 'scare'" class="flex size-full items-center justify-center">
      <img
        src="/jumpscare/scare.gif"
        alt=""
        class="size-4/5 object-cover [image-rendering:pixelated]"
      />
    </div>
    <div v-else class="relative size-full">
      <CameraFeed
        class="size-full rounded-none! border-0!"
        :noise="NOISE.intruder"
        rec="on"
        label="Cam 00"
        :stamp="stamp"
        :message="$t('intruder.title')"
        message-class="text-[48px] text-destructive sm:text-[88px]"
        :detail="$t('intruder.detail')"
      />
      <div class="absolute inset-x-0 bottom-[18%] flex justify-center">
        <button type="button" class="btn btn-primary" @click="reload">
          <RotateCw :size="18" aria-hidden="true" />
          {{ $t('intruder.reload') }}
        </button>
      </div>
    </div>
  </dialog>
</template>

<script lang="ts">
import { RotateCw } from 'lucide-vue-next'

import { NOISE } from '@/constants'

import type { IntruderAlertData } from '../components.types'
import CameraFeed from './CameraFeed.vue'

const SCARE_MS = 1_400
const SCREAM_GAIN = 4

export default {
  name: 'IntruderAlert',
  components: { CameraFeed, RotateCw },
  props: {
    open: {
      type: Boolean,
      required: true,
    },
    stamp: {
      type: String,
      default: '',
    },
  },
  data(): IntruderAlertData {
    return {
      phase: 'scare',
      scareTimer: undefined,
    }
  },
  computed: {
    NOISE: () => NOISE,
  },
  watch: {
    open(open: boolean) {
      if (open) {
        this.start()
      }
    },
  },
  beforeUnmount() {
    clearTimeout(this.scareTimer)
  },
  methods: {
    start() {
      const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      this.phase = reducedMotion ? 'alert' : 'scare'
      this.showDialog()
      this.scream()
      this.scareTimer = setTimeout(() => {
        this.phase = 'alert'
      }, SCARE_MS)
    },
    showDialog() {
      const dialog = this.$refs.dialog
      if (dialog instanceof HTMLDialogElement) {
        dialog.showModal()
      }
    },
    scream() {
      const audio = this.$refs.audio
      if (!(audio instanceof HTMLAudioElement)) {
        return
      }
      const context = new AudioContext()
      const gain = context.createGain()
      gain.gain.value = SCREAM_GAIN
      context.createMediaElementSource(audio).connect(gain).connect(context.destination)
      context.resume().catch(console.error)
      audio.play().catch(console.error)
    },
    reopen() {
      if (this.open) {
        this.showDialog()
      }
    },
    reload() {
      window.location.reload()
    },
  },
}
</script>
