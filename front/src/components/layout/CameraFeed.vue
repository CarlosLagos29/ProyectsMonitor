<template>
  <div class="relative overflow-hidden rounded-sm border border-border bg-feed">
    <slot />
    <StaticNoise v-if="noise > 0" :opacity="noise" />
    <div class="scanlines pointer-events-none absolute inset-0" aria-hidden="true" />
    <div
      v-if="$slots['top-left']"
      class="cam-label absolute top-2 left-3 flex items-center gap-1.5 text-[18px] text-foreground"
    >
      <slot name="top-left" />
    </div>
    <div
      v-if="$slots['top-right']"
      class="cam-label absolute top-2 right-3 text-[18px] text-muted-foreground"
    >
      <slot name="top-right" />
    </div>
    <div
      v-if="$slots['bottom-left']"
      class="cam-label absolute bottom-2 left-3 text-[18px] text-muted-foreground"
    >
      <slot name="bottom-left" />
    </div>
    <div
      v-if="$slots['bottom-right']"
      class="cam-label absolute right-3 bottom-2 text-[18px] text-muted-foreground"
    >
      <slot name="bottom-right" />
    </div>
    <div
      v-if="$slots.message"
      class="absolute inset-0 flex flex-col items-center justify-center gap-1 px-4 text-center font-cam text-[28px] leading-none text-foreground uppercase [text-shadow:0_1px_6px_#000000e6]"
    >
      <slot name="message" />
    </div>
  </div>
</template>

<script lang="ts">
import StaticNoise from './StaticNoise.vue'

export default {
  name: 'CameraFeed',
  components: { StaticNoise },
  props: {
    noise: {
      type: Number,
      default: 0,
    },
  },
}
</script>
