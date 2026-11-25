<template>
  <AppHeader v-if="$route.name" />
  <main class="mx-auto w-full max-w-300 space-y-5 px-4 py-6 sm:pt-8.5">
    <InfoNotice v-if="!serverUp" :tag="$t('serverWaking.tag')" tone="warning" dashed role="status">
      {{ $t('serverWaking.detail', { minutes: 1 }) }}
    </InfoNotice>
    <RouterView :key="viewKey" />
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
  viewKey: number
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
      viewKey: 0,
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
      if (!this.serverUp) {
        this.viewKey += 1
      }
      this.serverUp = true
      if (this.isActive && !this.limits) {
        this.loadMe().catch(console.error)
      }
    },
  },
}
</script>
