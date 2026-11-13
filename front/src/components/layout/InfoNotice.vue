<template>
  <aside class="rounded border border-amber-400 p-3 text-sm">
    <p v-if="title" class="font-semibold">{{ title }}</p>
    <slot />
  </aside>
</template>

<script lang="ts">
export default {
  name: 'InfoNotice',
  props: {
    title: {
      type: String,
      default: '',
    },
  },
}
</script>
