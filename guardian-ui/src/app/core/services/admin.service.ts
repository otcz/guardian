import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Invitacion, Pagina } from '../models/acceso.model';
import {
  Casa,
  CasaRequest,
  GrupoParametro,
  GuardiaPorteria,
  ImportacionCasas,
  ImportacionPersonas,
  Parametro,
  Persona,
  PersonaRegistrada,
  PersonaRequest,
  Porteria,
  PorteriaRequest,
  Resumen,
  ConteoSolicitudes,
  SolicitudCasaAdmin,
  SolicitudVehiculoAdmin,
  Usuario,
  Vehiculo,
  VehiculoRequest
} from '../models/admin.model';

/** Entidades sobre las que la administración puede echar el candado. */
export type RecursoBloqueable = 'personas' | 'vehiculos' | 'casas' | 'usuarios';

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly base = `${environment.apiUrl}/admin`;

  constructor(private readonly http: HttpClient) {}

  // ── Resumen ──────────────────────────────────────────────────────────────

  resumen(): Observable<Resumen> {
    return this.http.get<Resumen>(`${this.base}/resumen`);
  }

  // ── Casas ────────────────────────────────────────────────────────────────

  casas(): Observable<Casa[]> {
    return this.http.get<Casa[]>(`${this.base}/casas`);
  }

  crearCasa(request: CasaRequest): Observable<Casa> {
    return this.http.post<Casa>(`${this.base}/casas`, request);
  }

  actualizarCasa(id: number, request: CasaRequest): Observable<Casa> {
    return this.http.put<Casa>(`${this.base}/casas/${id}`, request);
  }

  cambiarEstadoCasa(id: number, activar: boolean): Observable<Casa> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Casa>(`${this.base}/casas/${id}/${accion}`, {});
  }

  /** El .xlsx de ejemplo, con los tipos de vivienda reales de esta sede. */
  plantillaCasas(): Observable<Blob> {
    return this.http.get(`${this.base}/casas/plantilla`, { responseType: 'blob' });
  }

  importarCasas(archivo: File): Observable<ImportacionCasas> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<ImportacionCasas>(`${this.base}/casas/importar`, cuerpo);
  }

  /** El .xlsx de ejemplo de personas, con las mismas columnas que se leen. */
  plantillaPersonas(): Observable<Blob> {
    return this.http.get(`${this.base}/personas/plantilla`, { responseType: 'blob' });
  }

  importarPersonas(archivo: File): Observable<ImportacionPersonas> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<ImportacionPersonas>(`${this.base}/personas/importar`, cuerpo);
  }

  // ── Solicitudes de casa ──────────────────────────────────────────────────
  //
  // Aprobar es lo que de verdad mete a alguien en un hogar, con sus vehículos
  // y sus invitaciones detrás. Por eso es del administrador y no del residente.

  solicitudesCasa(): Observable<SolicitudCasaAdmin[]> {
    return this.http.get<SolicitudCasaAdmin[]>(`${this.base}/solicitudes-casa`);
  }

  aprobarSolicitudCasa(id: number): Observable<SolicitudCasaAdmin> {
    return this.http.patch<SolicitudCasaAdmin>(
      `${this.base}/solicitudes-casa/${id}/aprobar`, {});
  }

  rechazarSolicitudCasa(id: number, motivo: string): Observable<SolicitudCasaAdmin> {
    return this.http.patch<SolicitudCasaAdmin>(
      `${this.base}/solicitudes-casa/${id}/rechazar`, { motivo });
  }

  // ── Solicitudes de vehículo ──────────────────────────────────────────────
  //
  // Aprobar acá es lo que hace que la talanquera suba para esa placa. Por eso
  // la decisión es del administrador y no del titular que la pide.

  solicitudesVehiculo(): Observable<SolicitudVehiculoAdmin[]> {
    return this.http.get<SolicitudVehiculoAdmin[]>(`${this.base}/solicitudes-vehiculo`);
  }

  aprobarSolicitudVehiculo(id: number): Observable<SolicitudVehiculoAdmin> {
    return this.http.patch<SolicitudVehiculoAdmin>(
      `${this.base}/solicitudes-vehiculo/${id}/aprobar`, {});
  }

  rechazarSolicitudVehiculo(id: number, motivo: string): Observable<SolicitudVehiculoAdmin> {
    return this.http.patch<SolicitudVehiculoAdmin>(
      `${this.base}/solicitudes-vehiculo/${id}/rechazar`, { motivo });
  }

  /**
   * El número del aviso del menú: las dos bandejas sumadas.
   *
   * <p>Endpoint propio y no las listas: esto se pide en cada navegación del
   * panel, y traer las solicitudes enteras para pintar un numerito sería pagar
   * la consulta grande todo el tiempo.</p>
   */
  solicitudesPendientes(): Observable<ConteoSolicitudes> {
    return this.http.get<ConteoSolicitudes>(`${this.base}/solicitudes/conteo`);
  }

  // ── Porterías ────────────────────────────────────────────────────────────
  //
  // Sin eliminar: cada evento de la bitácora apunta a la portería por la que
  // pasó la persona. Se desactiva, y el histórico sigue pudiendo responder.

  porterias(): Observable<Porteria[]> {
    return this.http.get<Porteria[]>(`${this.base}/porterias`);
  }

  crearPorteria(request: PorteriaRequest): Observable<Porteria> {
    return this.http.post<Porteria>(`${this.base}/porterias`, request);
  }

  actualizarPorteria(id: number, request: PorteriaRequest): Observable<Porteria> {
    return this.http.put<Porteria>(`${this.base}/porterias/${id}`, request);
  }

  cambiarEstadoPorteria(id: number, activar: boolean): Observable<Porteria> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Porteria>(`${this.base}/porterias/${id}/${accion}`, {});
  }

  guardiasDePorteria(id: number): Observable<GuardiaPorteria[]> {
    return this.http.get<GuardiaPorteria[]>(`${this.base}/porterias/${id}/guardias`);
  }

  /**
   * Reemplazo total: quedan EXACTAMENTE los que se manden. Con altas y bajas
   * sueltas, dos administradores editando a la vez dejan un estado que ninguno
   * de los dos eligió.
   */
  asignarGuardias(id: number, personaIds: number[]): Observable<Porteria> {
    return this.http.put<Porteria>(`${this.base}/porterias/${id}/guardias`, { personaIds });
  }

  // ── Personas ─────────────────────────────────────────────────────────────

  personas(texto?: string, pagina = 0, tamano = 25): Observable<Pagina<Persona>> {
    let params = new HttpParams()
      .set('pagina', String(pagina))
      .set('tamano', String(tamano));

    if (texto?.trim()) {
      params = params.set('texto', texto.trim());
    }
    return this.http.get<Pagina<Persona>>(`${this.base}/personas`, { params });
  }

  crearPersona(request: PersonaRequest): Observable<PersonaRegistrada> {
    return this.http.post<PersonaRegistrada>(`${this.base}/personas`, request);
  }

  actualizarPersona(id: number, request: PersonaRequest): Observable<Persona> {
    return this.http.put<Persona>(`${this.base}/personas/${id}`, request);
  }

  cambiarEstadoPersona(id: number, activar: boolean): Observable<Persona> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Persona>(`${this.base}/personas/${id}/${accion}`, {});
  }

  emitirCredencial(id: number): Observable<{ payload: string }> {
    return this.http.post<{ payload: string }>(`${this.base}/personas/${id}/credencial`, {});
  }

  /** Freno de emergencia: el QR muere en el próximo escaneo, sin reemitir. */
  revocarCredencial(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/personas/${id}/credencial`);
  }

  /** Eliminación física — exclusiva del administrador. */
  eliminarPersona(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/personas/${id}`);
  }

  // ── Vehículos ────────────────────────────────────────────────────────────

  vehiculos(casaId?: number): Observable<Vehiculo[]> {
    const params = casaId ? new HttpParams().set('casaId', String(casaId)) : undefined;
    return this.http.get<Vehiculo[]>(`${this.base}/vehiculos`, { params });
  }

  crearVehiculo(request: VehiculoRequest): Observable<Vehiculo> {
    return this.http.post<Vehiculo>(`${this.base}/vehiculos`, request);
  }

  actualizarVehiculo(id: number, request: VehiculoRequest): Observable<Vehiculo> {
    return this.http.put<Vehiculo>(`${this.base}/vehiculos/${id}`, request);
  }

  cambiarEstadoVehiculo(id: number, activar: boolean): Observable<Vehiculo> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Vehiculo>(`${this.base}/vehiculos/${id}/${accion}`, {});
  }

  /** Eliminación física — exclusiva del administrador. */
  eliminarVehiculo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/vehiculos/${id}`);
  }

  // ── Usuarios (cuentas de acceso) ─────────────────────────────────────────

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.base}/usuarios`);
  }

  crearUsuario(personaId: number, rol: string): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/usuarios`, { personaId, rol });
  }

  cambiarEstadoUsuario(id: number, activar: boolean): Observable<Usuario> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/${accion}`, {});
  }

  cambiarRolUsuario(id: number, rol: string): Observable<Usuario> {
    const params = new HttpParams().set('rol', rol);
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/rol`, {}, { params });
  }

  restablecerClave(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/restablecer-clave`, {});
  }

  /**
   * Clave elegida por la administración, para cuando volver a la inicial no
   * alcanza — por ejemplo si ya se supo cuál era y hay que cortar por lo sano.
   */
  asignarClave(id: number, claveNueva: string): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/clave`, { claveNueva });
  }

  // ── Bloqueos ─────────────────────────────────────────────────────────────

  /**
   * La OTRA llave. Activar/desactivar es del dueño; bloquear es del conjunto,
   * y por eso vive en un recurso propio y no como una acción más de cada panel.
   */
  bloquear(recurso: RecursoBloqueable, id: number, motivo: string): Observable<void> {
    return this.http.post<void>(`${this.base}/bloqueos/${recurso}/${id}`, { motivo });
  }

  desbloquear(recurso: RecursoBloqueable, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/bloqueos/${recurso}/${id}`);
  }

  // ── Invitaciones ─────────────────────────────────────────────────────────

  invitaciones(): Observable<Invitacion[]> {
    return this.http.get<Invitacion[]>(`${this.base}/invitaciones`);
  }

  revocarInvitacion(id: number): Observable<Invitacion> {
    return this.http.patch<Invitacion>(`${this.base}/invitaciones/${id}/revocar`, {});
  }

  /** La saca de la lista. La bitácora conserva los ingresos que permitió. */
  eliminarInvitacion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/invitaciones/${id}`);
  }

  // ── Catálogo ─────────────────────────────────────────────────────────────

  parametros(grupo: string): Observable<Parametro[]> {
    const params = new HttpParams().set('grupo', grupo);
    // Ruta comun, no /admin: los formularios del residente (parentesco, tipo
    // de vehiculo) necesitan los catalogos igual que el back-office, y bajo
    // /admin recibian 403.
    return this.http.get<Parametro[]>(`${environment.apiUrl}/parametros`, { params });
  }

  // ── Configuración del catálogo (solo administrador) ──────────────────────

  grupos(): Observable<GrupoParametro[]> {
    return this.http.get<GrupoParametro[]>(`${this.base}/parametros/grupos`);
  }

  /** Activas e inactivas: apagar una opción no puede equivaler a perderla. */
  opcionesDelGrupo(grupo: string): Observable<Parametro[]> {
    return this.http.get<Parametro[]>(`${this.base}/parametros/grupos/${grupo}`);
  }

  crearOpcion(grupo: string, valor: string): Observable<Parametro> {
    return this.http.post<Parametro>(`${this.base}/parametros/grupos/${grupo}`, { valor });
  }

  renombrarOpcion(id: number, valor: string): Observable<Parametro> {
    return this.http.put<Parametro>(`${this.base}/parametros/${id}`, { valor });
  }

  cambiarEstadoOpcion(id: number, activo: boolean): Observable<Parametro> {
    const params = new HttpParams().set('activo', activo);
    return this.http.put<Parametro>(`${this.base}/parametros/${id}/estado`, {}, { params });
  }
}
