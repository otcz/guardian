import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AccesoEvento,
  FichaVerificacion,
  MiQr,
  Pagina,
  Presencia,
  RegistrarAccesoRequest,
  Resultado
} from '../models/acceso.model';

@Injectable({ providedIn: 'root' })
export class AccesoService {

  private readonly base = `${environment.apiUrl}/acceso`;

  constructor(private readonly http: HttpClient) {}

  presencia(): Observable<Presencia> {
    return this.http.get<Presencia>(`${this.base}/presencia`);
  }

  verificar(payload: string): Observable<FichaVerificacion> {
    return this.http.post<FichaVerificacion>(`${this.base}/verificar`, { payload });
  }

  /**
   * El otro camino a la misma ficha: por documento y no por QR.
   *
   * <p>Lo usan los tres lectores de la portería — el de código de barras (del
   * PDF417 de una cédula sale el número, no un payload), la cédula tecleada y
   * la huella cuando la haya.</p>
   */
  verificarPorDocumento(documento: string): Observable<FichaVerificacion> {
    return this.http.post<FichaVerificacion>(
      `${this.base}/verificar-documento`, { documento });
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
