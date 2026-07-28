import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AccesoEvento,
  FichaVerificacion,
  MiQr,
  Pagina,
  RegistrarAccesoRequest,
  Resultado
} from '../models/acceso.model';

@Injectable({ providedIn: 'root' })
export class AccesoService {

  private readonly base = `${environment.apiUrl}/acceso`;

  constructor(private readonly http: HttpClient) {}

  verificar(payload: string): Observable<FichaVerificacion> {
    return this.http.post<FichaVerificacion>(`${this.base}/verificar`, { payload });
  }

  registrar(request: RegistrarAccesoRequest): Observable<AccesoEvento> {
    return this.http.post<AccesoEvento>(`${this.base}/registrar`, request);
  }

  eventos(filtros: {
    resultado?: Resultado | null;
    casaId?: number | null;
    pagina?: number;
    tamano?: number;
  } = {}): Observable<Pagina<AccesoEvento>> {

    let params = new HttpParams()
      .set('pagina', String(filtros.pagina ?? 0))
      .set('tamano', String(filtros.tamano ?? 50));

    if (filtros.resultado) {
      params = params.set('resultado', filtros.resultado);
    }
    if (filtros.casaId) {
      params = params.set('casaId', String(filtros.casaId));
    }

    return this.http.get<Pagina<AccesoEvento>>(`${this.base}/eventos`, { params });
  }

  miQr(): Observable<MiQr> {
    return this.http.get<MiQr>(`${environment.apiUrl}/residente/mi-qr`);
  }
}
