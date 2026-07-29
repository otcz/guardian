import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { InvitacionPublica } from '../models/acceso.model';

/**
 * La página que abre el INVITADO desde el link compartido. Sin sesión: el
 * interceptor no adjunta token porque no hay, y el backend lo permite.
 */
@Injectable({ providedIn: 'root' })
export class InvitacionPublicaService {

  constructor(private readonly http: HttpClient) {}

  ver(codigoPublico: string): Observable<InvitacionPublica> {
    return this.http.get<InvitacionPublica>(
      `${environment.apiUrl}/publico/invitaciones/${codigoPublico}`);
  }
}
