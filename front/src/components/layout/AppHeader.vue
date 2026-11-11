<template>
  <header class="flex items-center justify-between border-b px-4 py-3">
    <RouterLink :to="{ name: 'home' }" class="font-semibold">{{ $t('common.appName') }}</RouterLink>
    <div class="flex items-center gap-1" role="group" :aria-label="$t('common.language')">
      <button
        v-for="code in locales"
        :key="code"
        type="button"
        class="rounded px-2 py-1 text-sm"
        :class="{ 'bg-gray-200 font-semibold': isCurrent(code) }"
        :aria-pressed="isCurrent(code)"
        @click="changeLocale(code)"
      >
        {{ code.toUpperCase() }}
      </button>
    </div>
  </header>
</template>

<script lang="ts">
import { availableLocales, setLocale, type AppLocale } from '@/i18n'

export default {
  name: 'AppHeader',
  data() {
    return {
      locales: availableLocales,
    }
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
