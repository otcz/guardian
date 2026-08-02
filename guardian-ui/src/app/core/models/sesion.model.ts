export type Rol = 'SUPER_ADMIN' | 'ADMIN' | 'GUARDIA' | 'RESIDENTE';

export interface Sesion {
  usuarioId: number;
  personaId: number;
  documento: string;
  nombreCompleto: string;
  rol: Rol;
  fotoUrl: string | null;
  casaIdentificador: string | null;

  /** Sede sobre la que se está operando. Null para el super sin sede elegida. */
  sedeId: number | null;
  sedeNombre: string | null;

  /** true mientras un super administrador opera dentro de una sede ajena. */
  sedeSuplantada: boolean;
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
