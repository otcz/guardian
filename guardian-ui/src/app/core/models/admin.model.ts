/**
 * Clave con la que nace toda cuenta creada desde un panel — espejo de
 * `Codigos.CLAVE_INICIAL` en el backend.
 *
 * Vive acá porque las pantallas se la tienen que DECIR al administrador antes
 * de guardar: si el texto y el backend se separan, el administrador dicta una
 * clave que no funciona y la persona no entra.
 */
export const CLAVE_INICIAL = '0000';

export interface Casa {
  id: number;
  identificador: string;
  torre: string | null;
  numero: string;
  activo: string;
  bloqueado: string;
  motivoBloqueo: string | null;
  residentes: number;
  vehiculos: number;
}

export interface CasaRequest {
  torre?: string | null;
  numero: string;
  observaciones?: string | null;
}

/**
 * Punto de acceso del conjunto. En pantalla se llama portería, que es como lo
 * nombran el guardia y el residente.
 */
/** Resultado de una carga masiva, fila por fila. */
export interface ImportacionCasas {
  leidas: number;
  creadas: number;
  repetidas: number;
  conError: number;
  /** Solo las que NO se crearon: las buenas no hay que revisarlas. */
  rechazos: FilaRechazada[];
}

export interface FilaRechazada {
  /** Número de fila del Excel tal como se ve al abrirlo, con encabezado incluido. */
  fila: number;
  tipo: string;
  numero: string;
  motivo: string;
}

/** Mismo patrón que la carga de casas, con los datos de una persona. */
export interface ImportacionPersonas {
  leidas: number;
  creadas: number;
  repetidas: number;
  conError: number;
  rechazos: FilaPersonaRechazada[];
}

export interface FilaPersonaRechazada {
  fila: number;
  documento: string;
  nombre: string;
  motivo: string;
}

/**
 * Una casa como la ve alguien que todavía no vive en ninguna.
 *
 * <p>Sin nombres de residentes a propósito: quien elige casa aún no pertenece a
 * ninguna, y una lista con "CASA-101 — familia Pérez" le entregaría el
 * directorio del conjunto a cualquiera con cuenta.</p>
 */
export interface CasaDisponible {
  id: number;
  identificador: string;
  /** Dice si entraría como titular o a una familia que ya existe. */
  tieneTitular: boolean;
}

export interface SolicitudCasa {
  id: number;
  casaId: number;
  casaIdentificador: string;
  parentesco: string;
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
  motivoRechazo: string | null;
  fechaSolicitud: string;
}

/** La misma solicitud, en la bandeja del administrador. */
export interface SolicitudCasaAdmin {
  id: number;
  personaId: number;
  nombreCompleto: string;
  documento: string;
  casaIdentificador: string;
  parentesco: string;
  estado: string;
  fechaSolicitud: string;
  casaConTitular: boolean;
}

/**
 * Un vehículo pedido por el titular, todavía sin autorizar.
 *
 * <p>No es un vehículo: mientras esté acá no existe en la portería y no abre
 * ninguna talanquera. El vehículo nace cuando la administración aprueba.</p>
 */
export interface SolicitudVehiculo {
  id: number;
  placa: string;
  tipo: string;
  marca: string | null;
  color: string | null;
  tipoNombre: string | null;
  marcaNombre: string | null;
  colorNombre: string | null;
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
  motivoRechazo: string | null;
  fechaSolicitud: string;
}

/** La misma solicitud, en la bandeja del administrador. */
export interface SolicitudVehiculoAdmin {
  id: number;
  placa: string;
  tipoNombre: string | null;
  marcaNombre: string | null;
  colorNombre: string | null;
  casaId: number;
  casaIdentificador: string;
  solicitante: string;
  documento: string;
  estado: string;
  fechaSolicitud: string;
}

/** Lo que espera decisión, por bandeja y en total. El total es el aviso del menú. */
export interface ConteoSolicitudes {
  casas: number;
  vehiculos: number;
  pendientes: number;
}

export interface Porteria {
  id: number;
  nombre: string;
  direccion: string | null;
  permiteVehiculo: string;
  activo: string;
  /** Pasos registrados por acá. Dice si la portería está en uso real. */
  registros: number;
  /** Cuántos guardias tiene asignados. Los nombres se piden al abrir la hoja. */
  guardias: number;
}

export interface PorteriaRequest {
  nombre: string;
  direccion?: string | null;
  permiteVehiculo?: string | null;
}

/**
 * Una persona en la lista de guardias de una portería.
 *
 * <p>Es UNA sola lista con banderas y no dos —asignados y candidatos—: la
 * pantalla es una lista de chequeo, y partirla obligaría a fusionarla acá.</p>
 */
export interface GuardiaPorteria {
  personaId: number;
  nombreCompleto: string;
  documento: string;
  asignado: boolean;
  /**
   * Puede ser false y estar asignado: a alguien le cambiaron el rol después.
   * Se sigue viendo para poder quitarlo — filtrarlo lo volvería invisible.
   */
  esGuardia: boolean;
}

export interface Persona {
  id: number;
  tipoDocumento: string;
  documento: string;
  nombres: string;
  apellidos: string;
  nombreCompleto: string;
  fechaNacimiento: string | null;
  edad: number | null;
  fotoUrl: string | null;
  telefono: string | null;
  email: string | null;
  activo: string;
  bloqueado: string;
  motivoBloqueo: string | null;
  casaId: number | null;
  casaIdentificador: string | null;
  parentesco: string | null;
  tieneCredencial: boolean;

  /**
   * La cuenta de la aplicación, si la tiene. Todo null cuando no — que es el
   * caso de la mayoría: los menores y las empleadas entran con QR y nunca
   * abren la aplicación.
   */
  usuarioId: number | null;
  rol: string | null;
  usuarioActivo: string | null;
  usuarioBloqueado: string | null;
  usuarioMotivoBloqueo: string | null;
  usuarioUltimoIngreso: string | null;
}

export interface PersonaRequest {
  /** Código del grupo TIPO_DOCUMENTO (CC, TI, CE, PA, RC). */
  tipoDocumento?: string | null;
  documento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento?: string | null;
  fotoUrl?: string | null;
  telefono?: string | null;
  email?: string | null;
  casaId?: number | null;
  parentesco?: string | null;
  /** Si viene, el alta también crea la cuenta de acceso (inactiva). */
  rolUsuario?: string | null;
}

import { AccesoEvento } from './acceso.model';

/** Estado del conjunto de un vistazo — tablero de aterrizaje del admin. */
export interface Resumen {
  adentro: number;
  afuera: number;
  casasActivas: number;
  casasTotal: number;
  personasActivas: number;
  personasTotal: number;
  vehiculosActivos: number;
  vehiculosTotal: number;
  usuariosActivos: number;
  usuariosTotal: number;
  eventosHoy: number;
  permitidosHoy: number;
  denegadosHoy: number;
  ultimosMovimientos: AccesoEvento[];
}

export interface Usuario {
  id: number;
  personaId: number;
  documento: string;
  nombreCompleto: string;
  rol: string;
  activo: string;
  bloqueado: string;
  motivoBloqueo: string | null;
  requiereCambioClave: boolean;
  fechaUltimoIngreso: string | null;
}

/** Miembro de la casa en "Mi hogar". */
export interface Familiar {
  personaId: number;
  tipoDocumento: string;
  documento: string;
  nombreCompleto: string;
  parentesco: string;
  fotoUrl: string | null;
  edad: number | null;
  activo: string;
  bloqueado: string;
  motivoBloqueo: string | null;
  tieneCredencial: boolean;
  /** Si puede entrar a la aplicación. Sin cuenta solo existe para la portería. */
  tieneCuenta: boolean;
  /** Su último paso por la portería fue una ENTRADA: sigue en el conjunto. */
  adentro: boolean;
  esUsuarioActual: boolean;
}

export interface FamiliarRequest {
  tipoDocumento?: string | null;
  documento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento?: string | null;
  fotoUrl?: string | null;
  telefono?: string | null;
  /** Con correo, el familiar recibe cuenta y entra a la aplicación. */
  email?: string | null;
  parentesco: string;
}

export interface VehiculoResidenteRequest {
  placa: string;
  tipo: string;
  marca?: string | null;
  color?: string | null;
}

/** Respuesta del alta: incluye el QR para poder imprimir el carnet en el momento. */
export interface PersonaRegistrada {
  persona: Persona;
  payloadQr: string | null;
}

export interface Vehiculo {
  id: number;
  placa: string;
  /** Códigos del catálogo: son lo que se manda de vuelta al editar. */
  tipo: string;
  marca: string | null;
  color: string | null;
  /** Los mismos, ya traducidos por el backend. Es lo que se muestra. */
  tipoNombre: string | null;
  marcaNombre: string | null;
  colorNombre: string | null;
  activo: string;
  bloqueado: string;
  motivoBloqueo: string | null;
  casaId: number;
  casaIdentificador: string;
  /** Su último paso permitido fue una ENTRADA: el carro está en el conjunto. */
  adentro: boolean;
}

export interface VehiculoRequest {
  casaId: number;
  placa: string;
  tipo: string;
  marca?: string | null;
  color?: string | null;
}

export interface Parametro {
  id: number;
  grupo: string;
  codigo: string;
  valor: string;
  orden: number;
  /** El sistema la referencia por código: se renombra, no se desactiva. */
  protegido: boolean;
  activo: boolean;
}

/** Un grupo del catálogo tal como lo lista la pantalla de Configuración. */
export interface GrupoParametro {
  grupo: string;
  nombre: string;
  descripcion: string;
  opciones: number;
  /** false = solo se pueden renombrar sus opciones, no agregar ni quitar. */
  ampliable: boolean;
}

/** El código con el que el titular invita a su familia a registrarse sola. */
export interface CodigoHogar {
  codigo: string;
  vigenciaHasta: string;
  /** Sin usar y dentro de su vigencia. Solo entonces vale la pena compartirlo. */
  vigente: boolean;
  /** Quién lo usó, si ya se usó. Null mientras sigue esperando. */
  usadoPor: string | null;
}

/** Lo que ve quien abre el enlace, antes de registrarse. */
export interface HogarPublico {
  conjuntoNombre: string;
  casaIdentificador: string;
  titularNombre: string;
  vigente: boolean;
  /** Opciones del formulario: la pantalla no tiene sesión para pedirlas. */
  parentescos: Parametro[];
  tiposDocumento: Parametro[];
}

export interface RegistroHogarRequest {
  tipoDocumento?: string | null;
  documento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento?: string | null;
  telefono?: string | null;
  /** Obligatorio: quien se registra por acá siempre sale con cuenta. */
  email: string;
  parentesco: string;
}
