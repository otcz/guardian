export const environment = {
  production: true,
  // Mismo origen que el frontend: en produccion el API se sirve detras del
  // mismo dominio, asi no hay CORS ni un host que mantener sincronizado.
  apiUrl: '/api'
};
