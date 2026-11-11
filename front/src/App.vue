<template>
  <AppHeader />
  <main class="mx-auto w-full max-w-5xl p-4">
    <RouterView />
  </main>
</template>

<script lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'

export default {
  name: 'App',
  components: { AppHeader },
}
</script>
