<template>
  <section class="mx-auto max-w-190 space-y-6 py-6 text-center">
    <CameraFeed
      class="h-75 sm:h-95"
      :noise="0.32"
      rec="off"
      :label="`Cam ?? · ${$t('monitorDetail.notFound.label')}`"
      stamp="Err 404"
      :message="$t('monitorDetail.notFound.title')"
      message-tag="h1"
      message-class="text-[44px] text-foreground sm:text-[64px]"
      :detail="$t('monitorDetail.notFound.detail')"
    />
    <RouterLink :to="{ name: 'home' }" class="btn btn-primary">
      <ArrowLeft :size="18" aria-hidden="true" />
      {{ $t('monitorDetail.back') }}
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
