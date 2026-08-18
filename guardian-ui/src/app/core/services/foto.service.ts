import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, map, switchMap } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Lado mayor al que se reduce toda foto antes de subirla.
 *
 * <p>1600px es de sobra: la foto más grande que dibuja el sistema es la de la
 * ficha de la portería, y ahí ocupa unos 400px de ancho. Una cámara de iPhone
 * entrega 4032px y entre 4 y 12 MB — subir eso por datos móviles es lento, caro
 * para el residente, y no mejora en nada lo que el guardia ve.</p>
 */
const LADO_MAXIMO = 1600;

/** Calidad del JPEG resultante. 0.85 es el punto donde deja de notarse. */
const CALIDAD = 0.85;

@Injectable({ providedIn: 'root' })
export class FotoService {

  constructor(private readonly http: HttpClient) {}

  /** Sube la imagen y devuelve la URL relativa que se guarda en la persona. */
  subir(archivo: File): Observable<string> {
    return from(this.prepararImagen(archivo)).pipe(
      switchMap(preparada => {
        const datos = new FormData();
        datos.append('archivo', preparada, 'foto.jpg');

        return this.http
          .post<{ url: string }>(`${environment.apiUrl}/fotos`, datos)
          .pipe(map(respuesta => respuesta.url));
      })
    );
  }

  /**
   * Reduce y normaliza la foto ANTES de subirla.
   *
   * <p>Resuelve de raíz los dos motivos por los que una foto de teléfono
   * fallaba:</p>
   *
   * <ul>
   *   <li><b>El tamaño.</b> Una foto de iPhone pasa de 6 MB, que es el tope de
   *   multipart del servidor. Reventaba ANTES de llegar al controller, así que
   *   ni siquiera salía el mensaje de "foto muy pesada": salía un error
   *   inesperado.</li>
   *
   *   <li><b>El formato.</b> iOS guarda en HEIC, que el backend no acepta.
   *   Dibujar en un canvas y exportar convierte a JPEG cualquier cosa que el
   *   navegador sepa decodificar — y Safari sabe leer HEIC.</li>
   * </ul>
   *
   * <p>{@code imageOrientation: 'from-image'} es lo que evita el clásico de la
   * foto acostada: el teléfono guarda la orientación en los metadatos EXIF y no
   * en los píxeles, y un canvas que la ignore deja de lado a media portería.</p>
   *
   * <p>Si algo falla, se sube el archivo original: mejor un rechazo del
   * servidor con su mensaje que quedarse sin poder subir nada.</p>
   */
  private async prepararImagen(archivo: File): Promise<Blob> {
    try {
      const bitmap = await createImageBitmap(archivo, {
        imageOrientation: 'from-image'
      } as ImageBitmapOptions);

      const escala = Math.min(
        1, LADO_MAXIMO / Math.max(bitmap.width, bitmap.height));

      const lienzo = document.createElement('canvas');
      lienzo.width = Math.round(bitmap.width * escala);
      lienzo.height = Math.round(bitmap.height * escala);

      const pincel = lienzo.getContext('2d');
      if (!pincel) {
        return archivo;
      }
      pincel.drawImage(bitmap, 0, 0, lienzo.width, lienzo.height);
      bitmap.close();

      const blob = await new Promise<Blob | null>(
        resolver => lienzo.toBlob(resolver, 'image/jpeg', CALIDAD));

      return blob ?? archivo;
    } catch {
      return archivo;
    }
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
