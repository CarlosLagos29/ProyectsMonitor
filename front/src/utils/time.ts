import { i18n } from '@/i18n'

const MINUTE_MS = 60_000
const HOUR_MS = 60 * MINUTE_MS
const DAY_MS = 24 * HOUR_MS

export function formatElapsed(date: Date, now: number): string {
  const elapsed = now - date.getTime()
  if (elapsed < MINUTE_MS) {
    return i18n.global.t('time.secondsAgo')
  }
  if (elapsed < HOUR_MS) {
    return i18n.global.t('time.minutesAgo', { n: Math.floor(elapsed / MINUTE_MS) })
  }
  if (elapsed < DAY_MS) {
    return i18n.global.t('time.hoursAgo', { n: Math.floor(elapsed / HOUR_MS) })
  }
  return date.toLocaleString(i18n.global.locale.value)
}

export function formatRemaining(milliseconds: number): string {
  if (milliseconds < MINUTE_MS) {
    return i18n.global.t('time.underMinute')
  }
  const hours = Math.floor(milliseconds / HOUR_MS)
  const minutes = Math.floor((milliseconds % HOUR_MS) / MINUTE_MS)
  return hours > 0 ? `${hours} h ${minutes} min` : `${minutes} min`
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

export function formatStamp(date: Date): string {
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)} ${formatClock(date)}`
}

export function formatClock(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatHoursMinutes(milliseconds: number): string {
  const total = Math.max(0, Math.floor(milliseconds / MINUTE_MS))
  return `${pad(Math.floor(total / 60))}:${pad(total % 60)}`
}

export function formatCountdown(seconds: number): string {
  const total = Math.max(0, Math.ceil(seconds))
  return `${pad(Math.floor(total / 60))}:${pad(total % 60)}`
}

export function formatHoursMinutesSeconds(milliseconds: number): string {
  const total = Math.max(0, Math.floor(milliseconds / 1_000))
  return `${pad(Math.floor(total / 3600))}:${pad(Math.floor((total % 3600) / 60))}:${pad(total % 60)}`
}
