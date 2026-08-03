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

/** Paso 1 de "olvidé mi PIN". Se pide el documento, no el correo:
 *  es el dato con el que la persona ya inicia sesión. */
export interface SolicitarCodigoRequest {
  documento: string;
}

/** Respuesta idéntica exista o no la cuenta — a propósito, para que esta
 *  pantalla no sirva para averiguar qué cédulas viven en el conjunto. */
export interface SolicitudCodigoResponse {
  mensaje: string;
  minutosVigencia: number;
}

export interface RestablecerClaveRequest {
  documento: string;
  codigo: string;
  claveNueva: string;
}
