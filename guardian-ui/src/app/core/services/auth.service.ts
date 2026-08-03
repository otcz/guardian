import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CambiarClaveRequest,
  LoginRequest,
  LoginResponse,
  RestablecerClaveRequest,
  Rol,
  Sesion,
  SolicitarCodigoRequest,
  SolicitudCodigoResponse
} from '../models/sesion.model';
import { LLAVE_CACHE_MI_QR } from '../models/acceso.model';

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
      tap(respuesta => this.guardarSesion(respuesta))
    );
  }

  /**
   * El backend responde con una sesión NUEVA: el token anterior lleva la
   * autoridad degradada de clave pendiente y no sirve para nada más.
   */
  cambiarClave(request: CambiarClaveRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/cambiar-clave`, request)
      .pipe(tap(respuesta => this.guardarSesion(respuesta)));
  }

  // ── Olvidé mi PIN ─────────────────────────────────────────────────
  //
  // Bajo /publico y no bajo /auth: quien las usa no tiene sesión, que es
  // justamente el problema que viene a resolver.

  solicitarCodigo(request: SolicitarCodigoRequest): Observable<SolicitudCodigoResponse> {
    return this.http.post<SolicitudCodigoResponse>(
      `${environment.apiUrl}/publico/recuperacion/solicitar`, request);
  }

  restablecerClave(request: RestablecerClaveRequest): Observable<void> {
    return this.http.post<void>(
      `${environment.apiUrl}/publico/recuperacion/restablecer`, request);
  }

  cerrarSesion(): void {
    localStorage.removeItem(LLAVE_TOKEN);
    localStorage.removeItem(LLAVE_SESION);
    localStorage.removeItem(LLAVE_CAMBIO);
    // La credencial cacheada es del usuario, no del dispositivo: en una
    // tablet compartida, el siguiente en entrar no debe ver el QR del anterior.
    localStorage.removeItem(LLAVE_CACHE_MI_QR);
    this.sesionSubject.next(null);
    this.router.navigate(['/ingreso']);
  }

  /**
   * Reemplaza la sesión completa con la que devuelve el backend.
   *
   * Público a propósito: entrar y salir de una sede cambian el token, no solo
   * un dato de pantalla. Si solo se actualizara la sesión guardada, el token
   * seguiría apuntando a la sede anterior y el panel mostraría una sede
   * mientras el API opera sobre otra.
   */
  aplicarSesion(respuesta: LoginResponse): void {
    this.guardarSesion(respuesta);
  }

  private guardarSesion(respuesta: LoginResponse): void {
    localStorage.setItem(LLAVE_TOKEN, respuesta.token);
    localStorage.setItem(LLAVE_SESION, JSON.stringify(respuesta.usuario));
    localStorage.setItem(LLAVE_CAMBIO, String(respuesta.requiereCambioClave));
    this.sesionSubject.next(respuesta.usuario);
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

  /** true mientras el super administrador opera dentro de una sede ajena. */
  get sedeSuplantada(): boolean {
    return this.sesion?.sedeSuplantada === true;
  }

  /** Panel de arranque según el rol. Cada quien aterriza en el suyo. */
  rutaInicial(): string {
    switch (this.sesion?.rol) {
      case 'GUARDIA':
        return '/porteria';
      case 'ADMIN':
        return '/admin';
      case 'SUPER_ADMIN':
        // Dentro de una sede aterriza en el back-office de esa sede; fuera,
        // en el listado de sedes, que es lo único que puede hacer sin elegir.
        return this.sedeSuplantada ? '/admin' : '/sedes';
      default:
        return '/app';
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
