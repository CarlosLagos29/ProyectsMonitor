<template>
  <aside class="notice" :class="[borderClass, { 'border-dashed': dashed }]">
    <p>
      <span v-if="tag" class="notice-tag" :class="toneClass">[{{ tag }}]</span>
      <slot />
    </p>
  </aside>
</template>

<script lang="ts">
import type { PropType } from 'vue'

type NoticeTone = 'primary' | 'warning' | 'destructive'

const TONE_TEXT: Record<NoticeTone, string> = {
  primary: 'text-primary',
  warning: 'text-warning',
  destructive: 'text-destructive',
}

const TONE_BORDER: Record<NoticeTone, string> = {
  primary: 'border-border',
  warning: 'border-warning/60',
  destructive: 'border-destructive/70',
}

export default {
  name: 'InfoNotice',
  props: {
    tag: {
      type: String,
      default: '',
    },
    tone: {
      type: String as PropType<NoticeTone>,
      default: 'primary',
    },
    dashed: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    toneClass(): string {
      return TONE_TEXT[this.tone]
    },
    borderClass(): string {
      return TONE_BORDER[this.tone]
    },
  },
}
</script>
