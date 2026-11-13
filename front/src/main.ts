import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import { configureHttp } from './api/http'
import { i18n } from './i18n'
import router from './router'
import { useSessionStore } from './stores/session'

const app = createApp(App)

app.use(createPinia())

const session = useSessionStore()
session.scheduleExpiry()
configureHttp({
  getToken: () => session.token,
  onSessionExpired: () => session.expire(),
})

app.use(i18n)
app.use(router)

app.mount('#app')
