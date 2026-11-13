<template>
  <AppHeader />
  <main class="mx-auto w-full max-w-5xl space-y-4 p-4">
    <InfoNotice v-if="!serverUp" :title="$t('serverWaking.title')" role="status">
      <p>{{ $t('serverWaking.detail') }}</p>
    </InfoNotice>
    <RouterView />
  </main>
</template>

<script lang="ts">
import { mapActions, mapState } from 'pinia'

import { isServerUp } from '@/api/health'
import AppHeader from '@/components/layout/AppHeader.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import { useSessionStore } from '@/stores/session'

interface AppData {
  serverUp: boolean
  wakeTimer: ReturnType<typeof setTimeout> | undefined
  keepAliveTimer: ReturnType<typeof setInterval> | undefined
}

const WAKE_RETRY_MS = 5_000
const KEEP_ALIVE_MS = 10 * 60_000

export default {
  name: 'App',
  components: { AppHeader, InfoNotice },
  data(): AppData {
    return {
      serverUp: true,
      wakeTimer: undefined,
      keepAliveTimer: undefined,
    }
  },
  computed: {
    ...mapState(useSessionStore, ['isActive', 'limits']),
  },
  watch: {
    isActive(active: boolean) {
      if (!active) {
        void this.$router.push({ name: 'login' })
      }
    },
  },
  mounted() {
    void this.wakeServer()
    this.keepAliveTimer = setInterval(() => void this.keepAlive(), KEEP_ALIVE_MS)
  },
  beforeUnmount() {
    clearTimeout(this.wakeTimer)
    clearInterval(this.keepAliveTimer)
  },
  methods: {
    ...mapActions(useSessionStore, ['loadMe']),
    async wakeServer() {
      clearTimeout(this.wakeTimer)
      if (await isServerUp()) {
        this.onServerUp()
        return
      }
      this.serverUp = false
      this.wakeTimer = setTimeout(() => void this.wakeServer(), WAKE_RETRY_MS)
    },
    async keepAlive() {
      if (this.serverUp && !(await isServerUp())) {
        void this.wakeServer()
      }
    },
    onServerUp() {
      this.serverUp = true
      if (this.isActive && !this.limits) {
        this.loadMe().catch(() => undefined)
      }
    },
  },
}
</script>
