import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Invitacion, Pagina } from '../models/acceso.model';
import {
  Casa,
  CasaRequest,
  Parametro,
  Persona,
  PersonaRegistrada,
  PersonaRequest,
  Resumen,
  Usuario,
  Vehiculo,
  VehiculoRequest
} from '../models/admin.model';

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

  restablecerClave(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.base}/usuarios/${id}/restablecer-clave`, {});
  }

  // ── Invitaciones ─────────────────────────────────────────────────────────

  invitaciones(): Observable<Invitacion[]> {
    return this.http.get<Invitacion[]>(`${this.base}/invitaciones`);
  }

  revocarInvitacion(id: number): Observable<Invitacion> {
    return this.http.patch<Invitacion>(`${this.base}/invitaciones/${id}/revocar`, {});
  }

  // ── Catálogo ─────────────────────────────────────────────────────────────

  parametros(grupo: string): Observable<Parametro[]> {
    const params = new HttpParams().set('grupo', grupo);
    // Ruta comun, no /admin: los formularios del residente (parentesco, tipo
    // de vehiculo) necesitan los catalogos igual que el back-office, y bajo
    // /admin recibian 403.
    return this.http.get<Parametro[]>(`${environment.apiUrl}/parametros`, { params });
  }
}
