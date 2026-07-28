import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CambiarClaveRequest,
  LoginRequest,
  LoginResponse,
  Rol,
  Sesion
} from '../models/sesion.model';

const LLAVE_TOKEN = 'guardian.token';
const LLAVE_SESION = 'guardian.sesion';
const LLAVE_CAMBIO = 'guardian.requiereCambioClave';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly sesionSubject = new BehaviorSubject<Sesion | null>(this.leerSesion());

  /** Sesión actual como stream, para que el layout reaccione sin pedirla. */
  readonly sesion$ = this.sesionSubject.asObservable();

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap(respuesta => {
        localStorage.setItem(LLAVE_TOKEN, respuesta.token);
        localStorage.setItem(LLAVE_SESION, JSON.stringify(respuesta.usuario));
        localStorage.setItem(LLAVE_CAMBIO, String(respuesta.requiereCambioClave));
        this.sesionSubject.next(respuesta.usuario);
      })
    );
  }

  cambiarClave(request: CambiarClaveRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/cambiar-clave`, request).pipe(
      tap(() => localStorage.setItem(LLAVE_CAMBIO, 'false'))
    );
  }

  cerrarSesion(): void {
    localStorage.removeItem(LLAVE_TOKEN);
    localStorage.removeItem(LLAVE_SESION);
    localStorage.removeItem(LLAVE_CAMBIO);
    this.sesionSubject.next(null);
    this.router.navigate(['/ingreso']);
  }

  get token(): string | null {
    return localStorage.getItem(LLAVE_TOKEN);
  }

  get sesion(): Sesion | null {
    return this.sesionSubject.value;
  }

  get autenticado(): boolean {
    return this.token !== null;
  }

  get requiereCambioClave(): boolean {
    return localStorage.getItem(LLAVE_CAMBIO) === 'true';
  }

  tieneRol(...roles: Rol[]): boolean {
    const rol = this.sesion?.rol;
    return rol !== undefined && roles.includes(rol);
  }

  /** Pantalla de arranque según el rol. Cada quien entra a lo suyo. */
  rutaInicial(): string {
    switch (this.sesion?.rol) {
      case 'GUARDIA':
        return '/porteria';
      case 'ADMIN':
        return '/admin';
      default:
        return '/mi-qr';
    }
  }

  private leerSesion(): Sesion | null {
    const crudo = localStorage.getItem(LLAVE_SESION);
    if (!crudo) {
      return null;
    }
    try {
      return JSON.parse(crudo) as Sesion;
    } catch {
      // Si el storage quedó corrupto no vale la pena arrastrarlo: se limpia y
      // el usuario vuelve al login, que es un mal menor frente a una app que
      // no arranca.
      localStorage.removeItem(LLAVE_SESION);
      return null;
    }
  }
}
