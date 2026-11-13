const MINUTE_MS = 60_000
const HOUR_MS = 60 * MINUTE_MS
const DAY_MS = 24 * HOUR_MS

export function formatElapsed(date: Date, now: number): string {
  const elapsed = now - date.getTime()
  if (elapsed < MINUTE_MS) {
    return 'hace unos segundos'
  }
  if (elapsed < HOUR_MS) {
    return `hace ${Math.floor(elapsed / MINUTE_MS)} min`
  }
  if (elapsed < DAY_MS) {
    return `hace ${Math.floor(elapsed / HOUR_MS)} h`
  }
  return date.toLocaleString()
}

export function formatRemaining(milliseconds: number): string {
  if (milliseconds < MINUTE_MS) {
    return 'menos de un minuto'
  }
  const hours = Math.floor(milliseconds / HOUR_MS)
  const minutes = Math.floor((milliseconds % HOUR_MS) / MINUTE_MS)
  return hours > 0 ? `${hours} h ${minutes} min` : `${minutes} min`
}
