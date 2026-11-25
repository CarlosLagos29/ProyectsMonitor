<template>
  <div class="flex items-center" role="group" :aria-label="$t('common.language')">
    <button
      v-for="code in locales"
      :key="code"
      type="button"
      class="min-h-11 min-w-11 border border-border px-2 font-cam text-[20px] max-sm:min-h-9 max-sm:min-w-9 max-sm:text-[17px] leading-none first:rounded-l-sm last:rounded-r-sm"
      :class="
        isCurrent(code)
          ? 'border-primary bg-primary text-primary-foreground'
          : 'text-muted-foreground hover:text-foreground'
      "
      :aria-pressed="isCurrent(code)"
      @click="changeLocale(code)"
    >
      {{ code.toUpperCase() }}
    </button>
  </div>
</template>

<script lang="ts">
import { availableLocales, setLocale, type AppLocale } from '@/i18n'

export default {
  name: 'LanguageSwitch',
  computed: {
    locales: () => availableLocales,
  },
  methods: {
    isCurrent(code: AppLocale): boolean {
      return this.$i18n.locale === code
    },
    changeLocale(code: AppLocale) {
      setLocale(code)
    },
  },
}
</script>
