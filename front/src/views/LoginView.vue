<template>
  <section class="mx-auto max-w-110 space-y-5">
    <CameraFeed
      class="h-42.5 sm:h-50"
      rec="on"
      label="Cam 00 · Entrada"
      :stamp="clock"
      message="Projects Monitor"
      message-tag="h1"
      message-class="text-[40px] text-primary sm:text-[52px]"
      detail="Monitor de uptime del portfolio de Carlos Lagos"
    />

    <InfoNotice v-if="exitReason" tag="Sesión vencida" tone="warning" role="alert">
      Tu sesión expiró. Volvé a entrar.
    </InfoNotice>
    <InfoNotice
      v-if="guestBlock === 'RATE_LIMITED' && guestWaitSeconds > 0"
      tag="Bloqueo"
      tone="destructive"
      role="alert"
    >
      Demasiados intentos. Reintentá en
      <span class="font-cam text-[20px]" aria-live="polite">{{ guestCountdown }}</span
      >.
    </InfoNotice>
    <InfoNotice
      v-else-if="guestBlock === 'GUEST_CAPACITY_REACHED'"
      tag="Sin lugar"
      tone="warning"
      role="alert"
    >
      Todos los turnos de invitado están ocupados. Probá de nuevo en un rato.
    </InfoNotice>
    <InfoNotice v-else-if="guestError" tag="Error" tone="destructive" role="alert">
      {{ $t(guestError.key, guestError.params) }}
    </InfoNotice>

    <div class="panel space-y-3 p-5">
      <h2 class="text-lg font-semibold">¿Venís desde el portfolio?</h2>
      <button
        type="button"
        class="btn btn-primary min-h-15 w-full text-[30px]"
        :disabled="enteringAsGuest || guestBlocked"
        @click="enterAsGuest"
      >
        <Cctv :size="26" aria-hidden="true" />
        {{ enteringAsGuest ? 'Conectando…' : 'Empezar turno' }}
      </button>
      <p class="text-sm text-muted-foreground">
        Entrás como invitado: ves los monitores públicos y podés crear uno propio, que se borra solo
        a las 12 h.
      </p>
    </div>

    <div class="flex items-center gap-3 font-cam text-[18px] text-muted-foreground uppercase">
      <span class="h-px flex-1 bg-border" />
      O acceso de administrador
      <span class="h-px flex-1 bg-border" />
    </div>

    <form class="space-y-4" novalidate @submit.prevent="submitLogin">
      <div class="space-y-1">
        <label for="login-email" class="text-sm font-medium">Email</label>
        <input
          id="login-email"
          v-model.trim="email"
          type="email"
          autocomplete="username"
          class="field font-mono"
          :aria-invalid="emailInvalid"
          :aria-describedby="emailInvalid ? 'login-email-error' : undefined"
        />
        <p v-if="emailInvalid" id="login-email-error" class="field-error">
          <CircleAlert :size="14" aria-hidden="true" />
          Completá el email.
        </p>
      </div>
      <div class="space-y-1">
        <label for="login-password" class="text-sm font-medium">Contraseña</label>
        <div class="flex gap-2">
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            class="field"
            :aria-invalid="passwordInvalid"
            :aria-describedby="passwordInvalid ? 'login-password-error' : undefined"
          />
          <button
            type="button"
            class="btn btn-secondary btn-icon"
            :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            :aria-pressed="showPassword"
            @click="showPassword = !showPassword"
          >
            <EyeOff v-if="showPassword" :size="18" aria-hidden="true" />
            <Eye v-else :size="18" aria-hidden="true" />
          </button>
        </div>
        <p v-if="passwordInvalid" id="login-password-error" class="field-error">
          <CircleAlert :size="14" aria-hidden="true" />
          Completá la contraseña.
        </p>
      </div>
      <InfoNotice v-if="loginError" tag="Error" tone="destructive" role="alert">
        {{ $t(loginError.key, loginError.params) }}
      </InfoNotice>
      <button type="submit" class="btn btn-secondary w-full" :disabled="loggingIn">
        {{ loggingIn ? 'Entrando…' : 'Entrar' }}
      </button>
    </form>

    <div class="flex justify-center pt-2">
      <LanguageSwitch />
    </div>
  </section>
</template>

<script lang="ts">
import { Cctv, CircleAlert, Eye, EyeOff } from 'lucide-vue-next'
import { mapActions, mapState } from 'pinia'

import { describeError, toApiError } from '@/api/errors'
import CameraFeed from '@/components/layout/CameraFeed.vue'
import InfoNotice from '@/components/layout/InfoNotice.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import { useSessionStore } from '@/stores/session'
import { formatClock, formatCountdown } from '@/utils/time'

import type { LoginData } from './views.types'

const CLOCK_TICK_MS = 15_000
const DEFAULT_CAPACITY_WAIT_SECONDS = 60

export default {
  name: 'LoginView',
  components: { CameraFeed, Cctv, CircleAlert, Eye, EyeOff, InfoNotice, LanguageSwitch },
  data(): LoginData {
    return {
      email: '',
      password: '',
      showPassword: false,
      failedAttempts: 0,
      now: Date.now(),
      clockTimer: undefined,
      submitted: false,
      serverFieldErrors: {},
      loggingIn: false,
      loginError: null,
      enteringAsGuest: false,
      guestError: null,
      guestWaitSeconds: 0,
      guestBlock: null,
      guestTimer: undefined,
    }
  },
  computed: {
    ...mapState(useSessionStore, ['exitReason']),
    emailInvalid(): boolean {
      return (this.submitted && !this.email) || 'email' in this.serverFieldErrors
    },
    passwordInvalid(): boolean {
      return (this.submitted && !this.password) || 'password' in this.serverFieldErrors
    },
    clock(): string {
      return formatClock(new Date(this.now))
    },
    guestCountdown(): string {
      return formatCountdown(this.guestWaitSeconds)
    },
    guestBlocked(): boolean {
      return this.guestBlock !== null && this.guestWaitSeconds > 0
    },
  },
  mounted() {
    this.clockTimer = setInterval(() => {
      this.now = Date.now()
    }, CLOCK_TICK_MS)
  },
  beforeUnmount() {
    clearInterval(this.guestTimer)
    clearInterval(this.clockTimer)
  },
  methods: {
    ...mapActions(useSessionStore, ['login', 'startGuest']),
    async submitLogin() {
      this.submitted = true
      this.serverFieldErrors = {}
      this.loginError = null
      if (!this.email || !this.password) {
        return
      }
      this.loggingIn = true
      try {
        await this.login(this.email, this.password)
        await this.$router.push({ name: 'home' })
      } catch (error) {
        const apiError = toApiError(error)
        if (apiError.reason === 'INVALID_CREDENTIALS') {
          this.failedAttempts += 1
        }
        this.serverFieldErrors = apiError.fieldErrors
        this.loginError = describeError(apiError)
      } finally {
        this.loggingIn = false
      }
    },
    async enterAsGuest() {
      this.guestError = null
      this.enteringAsGuest = true
      try {
        await this.startGuest()
        await this.$router.push({ name: 'home' })
      } catch (error) {
        const apiError = toApiError(error)
        if (apiError.reason === 'RATE_LIMITED' || apiError.reason === 'GUEST_CAPACITY_REACHED') {
          this.blockGuest(apiError.reason, apiError.retryAfter ?? DEFAULT_CAPACITY_WAIT_SECONDS)
        } else {
          this.guestError = describeError(apiError)
        }
      } finally {
        this.enteringAsGuest = false
      }
    },
    blockGuest(reason: 'RATE_LIMITED' | 'GUEST_CAPACITY_REACHED', seconds: number) {
      clearInterval(this.guestTimer)
      this.guestBlock = reason
      this.guestWaitSeconds = seconds
      this.guestTimer = setInterval(() => {
        this.guestWaitSeconds -= 1
        if (this.guestWaitSeconds <= 0) {
          clearInterval(this.guestTimer)
          this.guestBlock = null
        }
      }, 1_000)
    },
  },
}
</script>
