<template>
  <header class="border-b border-border bg-card">
    <div class="mx-auto flex max-w-300 flex-wrap items-center gap-x-4 gap-y-2 px-4 py-2">
      <RouterLink
        :to="{ name: 'home' }"
        class="flex min-h-11 items-center gap-2 font-cam text-[28px] leading-none text-foreground uppercase"
      >
        <Cctv :size="24" class="text-primary" aria-hidden="true" />
        Projects Monitor
      </RouterLink>
      <p
        v-if="cameras !== null"
        class="hidden items-center gap-2 font-cam text-[20px] text-muted-foreground md:flex"
      >
        <span class="rec-dot" aria-hidden="true" />
        Live · {{ $t('header.cameras', cameras) }}
      </p>
      <div class="ml-auto flex items-center gap-2">
        <LanguageSwitch />
        <span v-if="isActive" class="chip min-h-11 font-cam text-[18px] uppercase">
          {{ $t('header.shift', { role: isAdmin ? $t('header.admin') : $t('header.guest') }) }}
        </span>
        <button
          v-if="isActive"
          type="button"
          class="btn btn-secondary max-sm:size-11 max-sm:px-0"
          :aria-label="$t('header.logout')"
          @click="logout"
        >
          <LogOut :size="18" aria-hidden="true" />
          <span class="max-sm:sr-only">{{ $t('header.logout') }}</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script lang="ts">
import { Cctv, LogOut } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import { useMonitorsStore } from '@/stores/monitors'
import { useSessionStore } from '@/stores/session'

import LanguageSwitch from './LanguageSwitch.vue'

export default {
  name: 'AppHeader',
  components: { Cctv, LanguageSwitch, LogOut },
  computed: {
    ...mapState(useSessionStore, ['isActive', 'isAdmin']),
    ...mapState(useMonitorsStore, ['page']),
    cameras(): number | null {
      return this.$route.name === 'home' && this.page ? this.page.totalElements : null
    },
  },
  methods: {
    ...mapActions(useSessionStore, ['logout']),
  },
}
</script>
