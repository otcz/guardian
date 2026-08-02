import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Familiar,
  FamiliarRequest,
  Vehiculo,
  VehiculoResidenteRequest
} from '../models/admin.model';
import { Invitacion, InvitacionRequest, MiQr } from '../models/acceso.model';

/**
 * Autogestión de "Mi hogar". Solo activar/inactivar: la eliminación es
 * exclusiva del administrador y este service ni siquiera la expone.
 */
@Injectable({ providedIn: 'root' })
export class ResidenteService {

  private readonly base = `${environment.apiUrl}/residente`;

  constructor(private readonly http: HttpClient) {}

  /**
   * El residente pone su propia foto y con eso se le emite la credencial.
   * Sin esto, quien quedo registrado sin foto dependia de que el
   * administrador se acordara de subirsela.
   */
  fijarMiFoto(fotoUrl: string): Observable<MiQr> {
    return this.http.put<MiQr>(`${this.base}/mi-foto`, { fotoUrl });
  }

  familia(): Observable<Familiar[]> {
    return this.http.get<Familiar[]>(`${this.base}/familia`);
  }

  agregarFamiliar(request: FamiliarRequest): Observable<unknown> {
    return this.http.post(`${this.base}/familia`, request);
  }

  cambiarEstadoFamiliar(personaId: number, activar: boolean): Observable<Familiar> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Familiar>(`${this.base}/familia/${personaId}/${accion}`, {});
  }

  vehiculos(): Observable<Vehiculo[]> {
    return this.http.get<Vehiculo[]>(`${this.base}/vehiculos`);
  }

  agregarVehiculo(request: VehiculoResidenteRequest): Observable<Vehiculo> {
    return this.http.post<Vehiculo>(`${this.base}/vehiculos`, request);
  }

  cambiarEstadoVehiculo(id: number, activar: boolean): Observable<Vehiculo> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Vehiculo>(`${this.base}/vehiculos/${id}/${accion}`, {});
  }

  // ── Invitaciones ─────────────────────────────────────────────────────────

  invitaciones(): Observable<Invitacion[]> {
    return this.http.get<Invitacion[]>(`${this.base}/invitaciones`);
  }

  crearInvitacion(request: InvitacionRequest): Observable<Invitacion> {
    return this.http.post<Invitacion>(`${this.base}/invitaciones`, request);
  }

  revocarInvitacion(id: number): Observable<Invitacion> {
    return this.http.patch<Invitacion>(`${this.base}/invitaciones/${id}/revocar`, {});
  }
}
