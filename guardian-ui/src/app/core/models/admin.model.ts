export interface Casa {
  id: number;
  identificador: string;
  torre: string | null;
  numero: string;
  cuposParqueadero: number | null;
  activo: string;
  residentes: number;
  vehiculos: number;
}

export interface CasaRequest {
  torre?: string | null;
  numero: string;
  cuposParqueadero?: number | null;
  observaciones?: string | null;
}

export interface Persona {
  id: number;
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
  casaId: number | null;
  casaIdentificador: string | null;
  parentesco: string | null;
  tieneCredencial: boolean;
  rol: string | null;
  usuarioActivo: string | null;
}

export interface PersonaRequest {
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

export interface Usuario {
  id: number;
  personaId: number;
  documento: string;
  nombreCompleto: string;
  rol: string;
  activo: string;
  requiereCambioClave: boolean;
  fechaUltimoIngreso: string | null;
}

/** Miembro de la casa en "Mi hogar". */
export interface Familiar {
  personaId: number;
  documento: string;
  nombreCompleto: string;
  parentesco: string;
  fotoUrl: string | null;
  edad: number | null;
  activo: string;
  tieneCredencial: boolean;
  esUsuarioActual: boolean;
}

export interface FamiliarRequest {
  documento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento?: string | null;
  fotoUrl?: string | null;
  telefono?: string | null;
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
  tipo: string;
  marca: string | null;
  color: string | null;
  activo: string;
  casaId: number;
  casaIdentificador: string;
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
  protegido: boolean;
}
