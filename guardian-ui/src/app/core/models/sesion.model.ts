export type Rol = 'ADMIN' | 'GUARDIA' | 'RESIDENTE';

export interface Sesion {
  usuarioId: number;
  personaId: number;
  documento: string;
  nombreCompleto: string;
  rol: Rol;
  fotoUrl: string | null;
  casaIdentificador: string | null;
}

export interface LoginRequest {
  documento: string;
  clave: string;
}

export interface LoginResponse {
  token: string;
  usuario: Sesion;
  requiereCambioClave: boolean;
}

export interface CambiarClaveRequest {
  claveActual: string;
  claveNueva: string;
}
