import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Adjunta el token a cada petición y traduce el 401 en un cierre de sesión.
 *
 * Sin lo segundo, un token vencido deja al usuario dando vueltas en pantallas
 * vacías sin entender por qué no carga nada.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private readonly auth: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.token;

    const peticion = token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

    return next.handle(peticion).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && this.auth.autenticado) {
          this.auth.cerrarSesion();
        }
        return throwError(() => error);
      })
    );
  }
}
