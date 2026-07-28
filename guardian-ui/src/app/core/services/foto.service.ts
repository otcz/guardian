import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FotoService {

  constructor(private readonly http: HttpClient) {}

  /** Sube la imagen y devuelve la URL relativa que se guarda en la persona. */
  subir(archivo: File): Observable<string> {
    const datos = new FormData();
    datos.append('archivo', archivo);

    return this.http
      .post<{ url: string }>(`${environment.apiUrl}/fotos`, datos)
      .pipe(map(respuesta => respuesta.url));
  }

  /**
   * Las fotos se guardan como ruta relativa (/api/publico/fotos/...) para que
   * el mismo dato sirva en dev y en prod; aquí se resuelve contra el host del
   * API activo.
   */
  urlAbsoluta(fotoUrl: string | null): string | null {
    if (!fotoUrl) {
      return null;
    }
    if (fotoUrl.startsWith('http')) {
      return fotoUrl;
    }
    const base = environment.apiUrl.replace(/\/api$/, '');
    return base + fotoUrl;
  }
}
