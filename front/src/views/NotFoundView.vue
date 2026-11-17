<template>
  <section class="mx-auto max-w-190 space-y-6 py-6 text-center">
    <CameraFeed
      class="h-75 sm:h-95"
      :noise="0.32"
      rec="off"
      label="Cam ?? · Sin señal"
      stamp="Err 404"
      message="Cámara no encontrada"
      message-tag="h1"
      message-class="text-[44px] text-foreground sm:text-[64px]"
      detail="La página que buscás no existe o la cámara fue desinstalada."
    />
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
