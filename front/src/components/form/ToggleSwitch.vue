<template>
  <div class="flex items-start justify-between gap-4">
    <div class="min-w-0">
      <p :id="`${id}-label`" class="text-sm font-medium">{{ label }}</p>
      <p v-if="help" :id="`${id}-help`" class="text-xs text-muted-foreground">{{ help }}</p>
    </div>
    <button
      :id="id"
      type="button"
      role="switch"
      class="relative inline-flex h-7 w-12 shrink-0 items-center rounded-full border transition-colors"
      :class="modelValue ? 'border-primary bg-primary' : 'border-switch-off bg-background'"
      :aria-checked="modelValue"
      :aria-labelledby="`${id}-label`"
      :aria-describedby="help ? `${id}-help` : undefined"
      @click="$emit('update:modelValue', !modelValue)"
    >
      <span
        class="inline-block size-5 rounded-full transition-transform"
        :class="
          modelValue ? 'translate-x-6 bg-primary-foreground' : 'translate-x-0.5 bg-muted-foreground'
        "
      />
    </button>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ToggleSwitch',
  props: {
    modelValue: {
      type: Boolean,
      required: true,
    },
    id: {
      type: String,
      required: true,
    },
    label: {
      type: String,
      required: true,
    },
    help: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue'],
}
</script>
