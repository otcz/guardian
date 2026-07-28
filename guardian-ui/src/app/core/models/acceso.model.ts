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
}

export interface MiQr {
  payload: string;
  nombreCompleto: string;
  documento: string;
  casaIdentificador: string | null;
  fotoUrl: string | null;
}

/** Envelope de paginacion de Spring Data. */
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
