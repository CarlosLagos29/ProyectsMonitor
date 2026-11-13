<template>
  <section class="mx-auto max-w-sm space-y-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">{{ $t('login.title') }}</h1>
      <p class="text-sm text-gray-600">{{ $t('login.intro') }}</p>
    </header>

    <p v-if="exitReason" role="alert" class="rounded border border-amber-400 p-2 text-sm">
      {{ $t(`login.${exitReason}`) }}
    </p>

    <form class="space-y-3" novalidate @submit.prevent="submitLogin">
      <label class="block">
        <span class="text-sm">{{ $t('login.email') }}</span>
        <input
          v-model.trim="email"
          type="email"
          autocomplete="username"
          class="mt-1 w-full rounded border px-2 py-1"
          :aria-invalid="emailInvalid"
        />
        <span v-if="emailInvalid" class="text-sm text-red-600">{{ $t('login.required') }}</span>
      </label>
      <label class="block">
        <span class="text-sm">{{ $t('login.password') }}</span>
        <input
          v-model="password"
          type="password"
          autocomplete="current-password"
          class="mt-1 w-full rounded border px-2 py-1"
          :aria-invalid="passwordInvalid"
        />
        <span v-if="passwordInvalid" class="text-sm text-red-600">{{ $t('login.required') }}</span>
      </label>
      <p v-if="loginError" role="alert" class="text-sm text-red-600">
        {{ $t(loginError.key, loginError.params) }}
      </p>
      <button type="submit" class="rounded border px-3 py-1" :disabled="loggingIn">
        {{ loggingIn ? $t('common.loading') : $t('login.submit') }}
      </button>
    </form>

    <div class="space-y-2 border-t pt-4">
      <p class="text-sm">{{ $t('login.guestIntro') }}</p>
      <button
        type="button"
        class="rounded border px-3 py-1"
        :disabled="enteringAsGuest || guestWaitSeconds > 0"
        @click="enterAsGuest"
      >
        {{ enteringAsGuest ? $t('common.loading') : $t('login.guest') }}
      </button>
      <p v-if="guestWaitSeconds > 0" role="alert" class="text-sm text-red-600">
        {{ $t('errors.RATE_LIMITED', { seconds: guestWaitSeconds }) }}
      </p>
      <p v-else-if="guestError" role="alert" class="text-sm text-red-600">
        {{ $t(guestError.key, guestError.params) }}
      </p>
    </div>
  </section>
</template>

<script lang="ts">
import { mapActions, mapState } from 'pinia'

import { describeError, toApiError } from '@/api/errors'
import { useSessionStore } from '@/stores/session'

import type { LoginData } from './views.types'

export default {
  name: 'LoginView',
  data(): LoginData {
    return {
      email: '',
      password: '',
      submitted: false,
      serverFieldErrors: {},
      loggingIn: false,
      loginError: null,
      enteringAsGuest: false,
      guestError: null,
      guestWaitSeconds: 0,
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
  },
  beforeUnmount() {
    clearInterval(this.guestTimer)
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
        if (apiError.reason === 'RATE_LIMITED' && apiError.retryAfter) {
          this.waitForGuest(apiError.retryAfter)
        } else {
          this.guestError = describeError(apiError)
        }
      } finally {
        this.enteringAsGuest = false
      }
    },
    waitForGuest(seconds: number) {
      clearInterval(this.guestTimer)
      this.guestWaitSeconds = seconds
      this.guestTimer = setInterval(() => {
        this.guestWaitSeconds -= 1
        if (this.guestWaitSeconds <= 0) {
          clearInterval(this.guestTimer)
        }
      }, 1_000)
    },
  },
}
</script>
