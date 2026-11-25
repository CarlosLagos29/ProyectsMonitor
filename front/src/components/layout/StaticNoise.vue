<template>
  <svg
    class="pointer-events-none absolute inset-0 size-full"
    aria-hidden="true"
    :style="{ opacity }"
  >
    <filter :id="filterId">
      <feTurbulence type="fractalNoise" baseFrequency="0.95" numOctaves="2" stitchTiles="stitch" />
      <feColorMatrix type="saturate" values="0" />
    </filter>
    <rect width="100%" height="100%" :filter="`url(#${filterId})`" />
  </svg>
</template>

<script lang="ts">
import { NOISE } from '@/constants'

import type { StaticNoiseData } from '../components.types'

let nextId = 0

export default {
  name: 'StaticNoise',
  props: {
    opacity: {
      type: Number,
      default: NOISE.down,
    },
  },
  data(): StaticNoiseData {
    nextId += 1
    return {
      filterId: `static-noise-${nextId}`,
    }
  },
}
</script>
