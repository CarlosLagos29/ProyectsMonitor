<template>
  <section class="mx-auto max-w-190 space-y-6 py-6 text-center">
    <CameraFeed class="h-75 sm:h-95" :noise="0.32">
      <template #top-left>
        <span class="rec-dot-off" aria-hidden="true" />
        Cam ?? · Sin señal
      </template>
      <template #top-right>Err 404</template>
      <template #message>
        <h1 class="text-[44px] sm:text-[64px]">Cámara no encontrada</h1>
        <p class="max-w-md font-sans text-base text-muted-foreground normal-case">
          La página que buscás no existe o la cámara fue desinstalada.
        </p>
      </template>
    </CameraFeed>
    <RouterLink :to="{ name: 'home' }" class="btn btn-primary">
      <ArrowLeft :size="18" aria-hidden="true" />
      Volver a las cámaras
    </RouterLink>
  </section>
</template>

<script lang="ts">
import { ArrowLeft } from 'lucide-vue-next'

import CameraFeed from '@/components/layout/CameraFeed.vue'

export default {
  name: 'NotFoundView',
  components: { ArrowLeft, CameraFeed },
}
</script>
