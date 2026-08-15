import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CasaDisponible,
  CodigoHogar,
  Familiar,
  FamiliarRequest,
  SolicitudCasa,
  SolicitudVehiculo,
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

  // ── Código para unirse al hogar ──────────────────────────────────────────

  /** Null cuando el hogar nunca ha generado uno. */
  codigoHogar(): Observable<CodigoHogar | null> {
    return this.http.get<CodigoHogar | null>(`${this.base}/hogar/codigo`);
  }

  generarCodigoHogar(): Observable<CodigoHogar> {
    return this.http.post<CodigoHogar>(`${this.base}/hogar/codigo`, {});
  }

  revocarCodigoHogar(): Observable<void> {
    return this.http.delete<void>(`${this.base}/hogar/codigo`);
  }

  // ── Todavía sin casa ─────────────────────────────────────────────────────
  //
  // Pedir NO asigna nada: deja constancia de quién pide qué, y la
  // administración decide. Entrar a un hogar de un clic sería autoservicio de
  // acceso — quien entra se queda con sus vehículos y sus invitaciones.

  casasDisponibles(): Observable<CasaDisponible[]> {
    return this.http.get<CasaDisponible[]>(`${this.base}/casas-disponibles`);
  }

  /** 204 cuando nunca ha pedido nada: el observable emite null. */
  miSolicitud(): Observable<SolicitudCasa | null> {
    return this.http.get<SolicitudCasa | null>(`${this.base}/solicitud-casa`);
  }

  solicitarCasa(casaId: number, parentesco: string): Observable<SolicitudCasa> {
    return this.http.post<SolicitudCasa>(
      `${this.base}/solicitud-casa`, { casaId, parentesco });
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

  cambiarEstadoVehiculo(id: number, activar: boolean): Observable<Vehiculo> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Vehiculo>(`${this.base}/vehiculos/${id}/${accion}`, {});
  }

  /**
   * La foto de un carro YA autorizado. `null` la quita.
   *
   * <p>Existe aparte de la solicitud porque el vehículo nace por aprobación:
   * si la foto solo se pudiera mandar al pedirlo, los carros autorizados antes
   * de que existiera este campo se quedarían sin foto para siempre.</p>
   */
  fijarFotoVehiculo(id: number, fotoUrl: string | null): Observable<Vehiculo> {
    return this.http.put<Vehiculo>(`${this.base}/vehiculos/${id}/foto`, { fotoUrl });
  }

  // ── Vehículos por autorizar ──────────────────────────────────────────────
  //
  // El titular no registra el vehículo: lo PIDE. Registrar una placa es darle
  // paso a un carro que el guardia no va a volver a discutir, así que lo
  // autoriza la administración.

  solicitudesVehiculo(): Observable<SolicitudVehiculo[]> {
    return this.http.get<SolicitudVehiculo[]>(`${this.base}/solicitudes-vehiculo`);
  }

  solicitarVehiculo(request: VehiculoResidenteRequest): Observable<SolicitudVehiculo> {
    return this.http.post<SolicitudVehiculo>(`${this.base}/solicitudes-vehiculo`, request);
  }

  /** Quita de la pantalla un rechazo que el residente ya leyó. */
  descartarSolicitudVehiculo(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/solicitudes-vehiculo/${id}/descartar`, {});
  }

  // ── Invitaciones ─────────────────────────────────────────────────────────

  invitaciones(): Observable<Invitacion[]> {
    return this.http.get<Invitacion[]>(`${this.base}/invitaciones`);
  }

  crearInvitacion(request: InvitacionRequest): Observable<Invitacion> {
    return this.http.post<Invitacion>(`${this.base}/invitaciones`, request);
  }

  /** Lo único que puede hacer el residente: eliminar es del super admin. */
  revocarInvitacion(id: number): Observable<Invitacion> {
    return this.http.patch<Invitacion>(`${this.base}/invitaciones/${id}/revocar`, {});
  }
}
