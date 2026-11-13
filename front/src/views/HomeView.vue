<template>
  <section class="space-y-4">
    <h1 class="text-xl font-semibold">{{ $t('home.title') }}</h1>
    <InfoNotice v-if="isGuest" title="Esto es una demo">
      <p>
        Projects Monitor es una app personal. Como invitado podés ver los monitores públicos y crear
        uno propio, que se borra a las 12 horas.
      </p>
    </InfoNotice>
    <p class="text-sm">{{ isAdmin ? $t('home.helloAdmin') : $t('home.helloGuest') }}</p>
  </section>
</template>

<script lang="ts">
import { mapState } from 'pinia'

import InfoNotice from '@/components/layout/InfoNotice.vue'
import { useSessionStore } from '@/stores/session'

export default {
  name: 'HomeView',
  components: { InfoNotice },
  computed: {
    ...mapState(useSessionStore, ['isAdmin', 'isGuest']),
  },
}
</script>
