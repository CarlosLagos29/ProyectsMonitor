export const checkReasonEn = {
  SUCCESS: 'The site is up and responding',
  TIMEOUT: 'The site took too long to respond',
  DNS_ERROR: 'The domain could not be resolved',
  CONNECTION_ERROR: 'Could not connect to the site',
  TLS_ERROR: "The site's TLS certificate is invalid or expired",
  HTTP_4XX: 'The site answered with a client error (4xx)',
  HTTP_5XX: 'The site answered with a server error (5xx)',
  TOO_MANY_REDIRECTS: 'The site redirected too many times',
  AUTH_REQUIRED: 'The site requires authentication',
  ACCESS_DENIED: 'The site denied access to the checker',
  PRIVATE_ADDRESS: 'Private or internal addresses are not allowed',
  PORT_NOT_ALLOWED: 'Only ports 80 and 443 are allowed',
}

export const checkReasonEs = {
  SUCCESS: 'El sitio está activo y responde',
  TIMEOUT: 'El sitio tardó demasiado en responder',
  DNS_ERROR: 'No se pudo resolver el dominio',
  CONNECTION_ERROR: 'No se pudo conectar con el sitio',
  TLS_ERROR: 'El certificado TLS del sitio es inválido o está vencido',
  HTTP_4XX: 'El sitio respondió con un error del cliente (4xx)',
  HTTP_5XX: 'El sitio respondió con un error del servidor (5xx)',
  TOO_MANY_REDIRECTS: 'El sitio redirigió demasiadas veces',
  AUTH_REQUIRED: 'El sitio requiere autenticación',
  ACCESS_DENIED: 'El sitio no dejó pasar al checker',
  PRIVATE_ADDRESS: 'No se permiten direcciones privadas o internas',
  PORT_NOT_ALLOWED: 'Solo se permiten los puertos 80 y 443',
}
