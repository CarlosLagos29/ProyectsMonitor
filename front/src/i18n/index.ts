import { createI18n } from 'vue-i18n'

import { messages, type MessageSchema } from './messages'

export type AppLocale = 'en' | 'es'

export const availableLocales: readonly AppLocale[] = ['en', 'es']

const STORAGE_KEY = 'locale'
const FALLBACK_LOCALE: AppLocale = 'en'

function isAppLocale(value: unknown): value is AppLocale {
  return typeof value === 'string' && (availableLocales as readonly string[]).includes(value)
}

function readStoredLocale(): AppLocale | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return isAppLocale(stored) ? stored : null
  } catch {
    return null
  }
}

function detectLocale(): AppLocale {
  return navigator.language.toLowerCase().startsWith('es') ? 'es' : FALLBACK_LOCALE
}

const initialLocale = readStoredLocale() ?? detectLocale()

export const i18n = createI18n<[MessageSchema], AppLocale, false>({
  legacy: false,
  locale: initialLocale,
  fallbackLocale: FALLBACK_LOCALE,
  messages,
})

document.documentElement.lang = initialLocale

export function setLocale(locale: AppLocale) {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
  try {
    localStorage.setItem(STORAGE_KEY, locale)
  } catch {
    return
  }
}
