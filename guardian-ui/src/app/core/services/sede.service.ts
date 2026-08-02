import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AdministradorSedeRequest, Sede, SedeRequest } from '../models/sede.model';
import { LoginResponse } from '../models/sesion.model';
import { AuthService } from './auth.service';

/**
 * Panel de la plataforma — solo el super administrador llega acá.
 *
 * Entrar a una sede NO manda un parámetro: pide una sesión nueva y reemplaza
 * la actual. Así la sede siempre viaja en el token y ningún endpoint de
 * administración tiene que aceptarla como dato del cliente.
 */
@Injectable({ providedIn: 'root' })
export class SedeService {

  private readonly base = `${environment.apiUrl}/super/sedes`;

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService
  ) {}

  sedes(): Observable<Sede[]> {
    return this.http.get<Sede[]>(this.base);
  }

  crear(request: SedeRequest): Observable<Sede> {
    return this.http.post<Sede>(this.base, request);
  }

  actualizar(id: number, request: SedeRequest): Observable<Sede> {
    return this.http.put<Sede>(`${this.base}/${id}`, request);
  }

  cambiarEstado(id: number, activar: boolean): Observable<Sede> {
    const accion = activar ? 'activar' : 'desactivar';
    return this.http.patch<Sede>(`${this.base}/${id}/${accion}`, {});
  }

  crearAdministrador(id: number, request: AdministradorSedeRequest): Observable<Sede> {
    return this.http.post<Sede>(`${this.base}/${id}/administrador`, request);
  }

  entrar(id: number): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.base}/${id}/entrar`, {})
      .pipe(tap(respuesta => this.auth.aplicarSesion(respuesta)));
  }

  salir(): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.base}/salir`, {})
      .pipe(tap(respuesta => this.auth.aplicarSesion(respuesta)));
  }
}
