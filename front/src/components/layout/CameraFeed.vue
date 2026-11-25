<template>
  <div class="relative overflow-hidden rounded-sm border border-border bg-feed">
    <slot />
    <StaticNoise v-if="noise > 0" :opacity="noise" />
    <div class="scanlines pointer-events-none absolute inset-0" aria-hidden="true" />
    <p
      v-if="label"
      class="cam-label absolute top-2 left-3 flex items-center gap-1.5 text-[18px] text-foreground"
    >
      <span v-if="rec" :class="rec === 'on' ? 'rec-dot' : 'rec-dot-off'" aria-hidden="true" />
      {{ label }}
    </p>
    <p v-if="stamp" class="cam-label absolute top-2 right-3 text-[18px] text-muted-foreground">
      {{ stamp }}
    </p>
    <p
      v-if="bottomLeft"
      class="cam-label absolute bottom-2 left-3 text-[18px] text-muted-foreground"
    >
      {{ bottomLeft }}
    </p>
    <p
      v-if="bottomRight"
      class="cam-label absolute right-3 bottom-2 text-[18px] text-muted-foreground"
    >
      {{ bottomRight }}
    </p>
    <div
      v-if="message"
      class="absolute inset-0 flex flex-col items-center justify-center gap-2 px-4 text-center"
    >
      <h1
        v-if="messageTag === 'h1'"
        class="font-cam leading-none uppercase [text-shadow:0_1px_6px_#000000e6]"
        :class="messageClass"
      >
        {{ message }}
      </h1>
      <p
        v-else
        class="font-cam leading-none uppercase [text-shadow:0_1px_6px_#000000e6]"
        :class="messageClass"
      >
        {{ message }}
      </p>
      <p v-if="detail" class="max-w-md text-sm text-balance text-muted-foreground">{{ detail }}</p>
    </div>
  </div>
</template>

<script lang="ts">
import type { PropType } from 'vue'

import StaticNoise from './StaticNoise.vue'

export default {
  name: 'CameraFeed',
  components: { StaticNoise },
  props: {
    noise: {
      type: Number,
      default: 0,
    },
    rec: {
      type: String as PropType<'on' | 'off' | ''>,
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
    stamp: {
      type: String,
      default: '',
    },
    bottomLeft: {
      type: String,
      default: '',
    },
    bottomRight: {
      type: String,
      default: '',
    },
    message: {
      type: String,
      default: '',
    },
    detail: {
      type: String,
      default: '',
    },
    messageTag: {
      type: String as PropType<'p' | 'h1'>,
      default: 'p',
    },
    messageClass: {
      type: String,
      default: 'text-[28px] text-foreground',
    },
  },
}
</script>
