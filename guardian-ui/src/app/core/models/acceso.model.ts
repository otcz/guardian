export type Sentido = 'E' | 'S';
export type Modo = 'PEATON' | 'VEHICULO';
export type Resultado = 'PERMITIDO' | 'DENEGADO';

export interface VehiculoResumen {
  id: number;
  placa: string;
  tipo: string;
  marca: string | null;
  color: string | null;
}

/** Lo que el guardia ve tras escanear. Refleja FichaVerificacionResponse del API. */
export interface FichaVerificacion {
  permitido: boolean;
  motivoDenegacion: string | null;
  mensaje: string | null;
  fotoUrl: string | null;
  nombreCompleto: string | null;
  documento: string | null;
  casaIdentificador: string | null;
  edad: number | null;
  sentidoSugerido: Sentido | null;
  vehiculos: VehiculoResumen[];
  esInvitado: boolean;
  anfitrionNombre: string | null;
  invitacionPlaca: string | null;
  payload: string | null;
}

export interface RegistrarAccesoRequest {
  payload: string;
  modo: Modo;
  vehiculoId?: number | null;
  sentido?: Sentido | null;
  puntoAccesoId?: number | null;
}

export interface AccesoEvento {
  id: number;
  fechaEvento: string;
  sentido: Sentido;
  modo: Modo;
  resultado: Resultado;
  motivoDenegacion: string | null;
  personaNombre: string | null;
  personaDocumento: string | null;
  casaIdentificador: string | null;
  vehiculoPlaca: string | null;
  guardiaNombre: string | null;
  puntoAcceso: string | null;
  invitado: boolean;
}

export type EstadoInvitacion =
  'VIGENTE' | 'NO_VIGENTE' | 'AGOTADA' | 'VENCIDA' | 'REVOCADA';

export interface Invitacion {
  id: number;
  nombreInvitado: string;
  documentoInvitado: string;
  placa: string | null;
  vigenciaDesde: string;
  vigenciaHasta: string;
  usosMaximos: number;
  usosRealizados: number;
  estado: EstadoInvitacion;
  casaIdentificador: string;
  anfitrionNombre: string;
  codigoPublico: string;
  payload: string;
}

export interface InvitacionRequest {
  nombreInvitado: string;
  documentoInvitado: string;
  placa?: string | null;
  vigenciaDesde?: string | null;
  vigenciaHasta?: string | null;
  usosMaximos?: number | null;
}

/** Lo que ve el invitado al abrir el link, sin sesión. */
export interface InvitacionPublica {
  nombreInvitado: string;
  casaIdentificador: string;
  anfitrionNombre: string;
  vigenciaDesde: string;
  vigenciaHasta: string;
  usosRestantes: number;
  estado: EstadoInvitacion;
  payload: string;
}

export interface MiQr {
  payload: string;
  nombreCompleto: string;
  documento: string;
  casaIdentificador: string | null;
  fotoUrl: string | null;
}

/** Contadores que encabezan la portería. */
export interface Presencia {
  adentro: number;
  afuera: number;
  totalActivos: number;
}

/** Envelope de paginacion de Spring Data. */
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
