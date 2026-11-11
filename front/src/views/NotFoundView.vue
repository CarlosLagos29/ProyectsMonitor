<template>
  <h1 class="text-xl font-semibold">{{ $t('notFound.title') }}</h1>
  <RouterLink :to="{ name: 'home' }" class="text-sm underline">{{
    $t('notFound.backHome')
  }}</RouterLink>
</template>

<script lang="ts">
export default {
  name: 'NotFoundView',
}
</script>
