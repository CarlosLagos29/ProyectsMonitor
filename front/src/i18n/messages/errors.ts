import { defineMessages } from './define'

export default defineMessages({
  en: {
    VALIDATION_ERROR: 'Some fields are invalid.',
    INVALID_URL: 'The URL is not valid.',
    INVALID_API_TOKEN: 'The app is misconfigured. Please try again later.',
    INVALID_CREDENTIALS: 'Wrong email or password.',
    SESSION_EXPIRED: 'Your session expired. Please sign in again.',
    FORBIDDEN_FOR_ROLE: "You can't do that with your account.",
    NOT_FOUND: "This monitor doesn't exist or has expired.",
    MONITOR_LIMIT_REACHED: 'You reached your monitor limit.',
    RATE_LIMITED: 'Too many requests. Try again in {seconds} s.',
    GUEST_CAPACITY_REACHED: 'The demo is full right now. Try again in {minutes} min.',
    NETWORK: "Can't reach the server.",
    GENERIC: 'Something went wrong. Please try again.',
  },
  es: {
    VALIDATION_ERROR: 'Hay campos inválidos.',
    INVALID_URL: 'La URL no es válida.',
    INVALID_API_TOKEN: 'La app está mal configurada. Probá más tarde.',
    INVALID_CREDENTIALS: 'Email o contraseña incorrectos.',
    SESSION_EXPIRED: 'Tu sesión venció. Volvé a ingresar.',
    FORBIDDEN_FOR_ROLE: 'Tu cuenta no puede hacer eso.',
    NOT_FOUND: 'Este monitor no existe o ya venció.',
    MONITOR_LIMIT_REACHED: 'Llegaste a tu límite de monitores.',
    RATE_LIMITED: 'Demasiados pedidos. Probá de nuevo en {seconds} s.',
    GUEST_CAPACITY_REACHED: 'La demo está llena. Probá de nuevo en {minutes} min.',
    NETWORK: 'No se puede conectar con el servidor.',
    GENERIC: 'Algo salió mal. Probá de nuevo.',
  },
})
