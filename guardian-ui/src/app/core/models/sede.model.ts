/** Una sede vista desde el panel de la plataforma. */
export interface Sede {
  id: number;
  nombre: string;
  nit: string | null;
  direccion: string | null;
  telefono: string | null;
  activo: string;
  bloqueado: string;
  casas: number;
  personas: number;
  usuarios: number;
  /** false cuando la sede todavía no tiene a quien administrarla. */
  tieneAdministrador: boolean;
}

export interface SedeRequest {
  nombre: string;
  nit?: string | null;
  direccion?: string | null;
  telefono?: string | null;
}

/** El primer administrador de una sede. Nace activo y con cambio de clave. */
export interface AdministradorSedeRequest {
  documento: string;
  nombres: string;
  apellidos: string;
  telefono?: string | null;
  email?: string | null;
}
