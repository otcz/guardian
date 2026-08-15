export type Sentido = 'E' | 'S';
export type Modo = 'PEATON' | 'VEHICULO';
export type Resultado = 'PERMITIDO' | 'DENEGADO';

/**
 * Llave del payload de la credencial cacheado en el dispositivo. Exportada
 * para que cerrar sesión pueda borrarla: en una tablet compartida, el QR del
 * usuario anterior no debe sobrevivir a su sesión.
 */
export const LLAVE_CACHE_MI_QR = 'guardian.miQr';

export interface VehiculoResumen {
  id: number;
  placa: string;
  tipo: string;
  marca: string | null;
  color: string | null;
  /**
   * El carro, para que el guardia elija mirando y no leyendo. Null es
   * legítimo: al vehículo lo identifica su placa, y sin foto el botón cae al
   * icono genérico.
   */
  fotoUrl: string | null;
}

/** Lo que el guardia ve tras escanear. Refleja FichaVerificacionResponse del API. */
export interface FichaVerificacion {
  permitido: boolean;
  motivoDenegacion: string | null;
  mensaje: string | null;
  /**
   * Lo que escribió la administración al deshabilitar, tal cual. Null cuando
   * la denegación no viene de un bloqueo (credencial vencida, firma inválida).
   */
  motivoBloqueo: string | null;
  fotoUrl: string | null;
  nombreCompleto: string | null;
  /** Null en invitados: declaran solo el número. */
  tipoDocumento: string | null;
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
  /** true solo cuando el guardia contradice conscientemente el sentido inferido. */
  corregirSentido?: boolean | null;
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

/**
 * Alguien ofrecido al guardia cuando busca por nombre.
 *
 * <p>Es el respaldo del escaneo: el QR no carga, el teléfono se quedó sin
 * batería, la persona llegó sin cédula. Elegirlo NO autoriza nada — dispara la
 * verificación normal por documento, que es la que decide.</p>
 */
export interface CandidatoGarita {
  personaId: number;
  nombreCompleto: string;
  documento: string;
  casaIdentificador: string | null;
  fotoUrl: string | null;
}

/**
 * Alguien que está adentro o afuera ahora mismo.
 *
 * <p>Sale del último paso permitido de cada persona, con el nombre y la casa
 * copiados en ese evento: responde por lo que quedó registrado, no por lo que
 * dice el registro de la persona hoy.</p>
 */
export interface QuienEsta {
  personaId: number | null;
  nombreCompleto: string | null;
  casaIdentificador: string | null;
  desde: string;
  vehiculoPlaca: string | null;
  invitado: boolean;
}

/** Si el módulo de huella opera del lado del servidor. */
export interface EstadoHuella {
  disponible: boolean;
  algoritmo: string;
  maximoDedos: number;
  capturasPorDedo: number;
}

/**
 * Los dedos registrados de una persona.
 *
 * <p>NO viaja la plantilla, y es deliberado: es un dato biométrico y no tiene
 * por qué salir del servidor. La pantalla solo necesita saber cuáles hay para
 * no ofrecer el mismo dedo dos veces.</p>
 */
export interface HuellasDeUnaPersona {
  personaId: number;
  nombreCompleto: string;
  dedos: DedoRegistrado[];
  puedeAgregar: boolean;
}

export interface DedoRegistrado {
  dedo: string;
  calidad: number | null;
  fechaRegistro: string;
  algoritmo: string;
}

export type EstadoInvitacion =
  'PENDIENTE' | 'RECHAZADA'
  | 'VIGENTE' | 'NO_VIGENTE' | 'AGOTADA' | 'VENCIDA' | 'REVOCADA';

export interface Invitacion {
  id: number;
  nombreInvitado: string;
  documentoInvitado: string;
  placa: string | null;
  vigenciaDesde: string;
  vigenciaHasta: string;
  estado: EstadoInvitacion;
  /** Solo si estado es RECHAZADA. */
  motivoRechazo: string | null;
  casaIdentificador: string;
  anfitrionNombre: string;
  /** Su último paso por la portería fue una ENTRADA: sigue en el conjunto. */
  adentro: boolean;
  codigoPublico: string;
  payload: string;
}

export interface InvitacionRequest {
  nombreInvitado: string;
  documentoInvitado: string;
  placa?: string | null;
  /** La ventana de la visita. Sin tope de entradas: dentro de ella entra y sale. */
  vigenciaDesde?: string | null;
  vigenciaHasta?: string | null;
}

/** Lo que ve el invitado al abrir el link, sin sesión. */
export interface InvitacionPublica {
  nombreInvitado: string;
  casaIdentificador: string;
  anfitrionNombre: string;
  vigenciaDesde: string;
  vigenciaHasta: string;
  estado: EstadoInvitacion;
  payload: string;
}

export interface MiQr {
  /** Null mientras falte la foto: sin ella no hay credencial que emitir. */
  payload: string | null;

  /** true cuando lo unico que falta es la foto. Es un estado, no un error. */
  necesitaFoto: boolean;

  nombreCompleto: string;
  tipoDocumento: string;
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
