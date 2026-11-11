<template>
  <h1 class="text-xl font-semibold">{{ $t('monitorDetail.title') }}</h1>
  <p class="text-sm text-gray-500">{{ id }}</p>
</template>

<script lang="ts">
export default {
  name: 'MonitorDetailView',
  props: {
    id: {
      type: String,
      required: true,
    },
  },
}
</script>
