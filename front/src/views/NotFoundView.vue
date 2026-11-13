<template>
  <h1 class="text-xl font-semibold">Error 404</h1>
  <RouterLink :to="{ name: 'home' }" class="text-sm underline">Volver al inicio</RouterLink>
</template>

<script lang="ts">
export default {
  name: 'NotFoundView',
}
</script>
