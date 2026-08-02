import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { HogarPublico, RegistroHogarRequest } from '../models/admin.model';

/**
 * Registro dentro de un hogar existente. Sin sesión: el código UUID es la
 * llave, igual que el enlace del invitado.
 */
@Injectable({ providedIn: 'root' })
export class HogarPublicoService {

  private readonly base = `${environment.apiUrl}/publico/hogar`;

  constructor(private readonly http: HttpClient) {}

  consultar(codigo: string): Observable<HogarPublico> {
    return this.http.get<HogarPublico>(`${this.base}/${codigo}`);
  }

  registrar(codigo: string, request: RegistroHogarRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/${codigo}`, request);
  }
}
