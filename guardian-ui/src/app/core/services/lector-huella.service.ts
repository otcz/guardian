import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';

/** Una lectura del sensor. La plantilla viaja en base64 porque es binaria. */
export interface LecturaHuella {
  plantilla: string;
  /** Lo que reportó el lector, de 0 a 100. Sirve para diagnosticar después. */
  calidad: number;
}

/**
 * LA COSTURA DEL FRONTEND. Lo único que depende del lector que se compre.
 *
 * <p>Todo lo demás del módulo de huella —la pantalla, los tres pasos, buscar a
 * la persona, guardar, borrar— está construido y no cambia. Cuando llegue el
 * lector se implementa este servicio y el módulo queda operando.</p>
 *
 * <p><b>Por qué un servicio y no una llamada suelta.</b> Un navegador NO puede
 * hablarle a un sensor USB por su cuenta: lo hace a través del servicio local
 * que instala el fabricante, que escucha en <code>localhost</code>. Ese detalle
 * —qué puerto, qué protocolo, si es HTTP o WebSocket— cambia con cada marca, y
 * encerrarlo aquí es lo que evita que la marca del lector se filtre por toda la
 * pantalla de la portería.</p>
 *
 * <p><b>Cómo se implementa cuando llegue.</b> Con el ZKFinger WebSDK de ZKTeco
 * queda algo así:</p>
 *
 * <pre>
 *   capturar(): Observable&lt;LecturaHuella&gt; {
 *     return this.http.post&lt;...&gt;('http://localhost:PUERTO/capture', {...})
 *       .pipe(map(r =&gt; ({ plantilla: r.template, calidad: r.quality })));
 *   }
 * </pre>
 *
 * <p>Y <code>disponible()</code> pasa a preguntarle al servicio local si está
 * vivo, en vez de devolver false.</p>
 */
@Injectable({ providedIn: 'root' })
export class LectorHuellaService {

  /**
   * Si hay lector conectado y su servicio local responde.
   *
   * <p>Hoy siempre false. La pantalla lo consulta para decir "sensor no
   * conectado" en vez de ofrecer un botón que no hace nada — que en una
   * portería le quema al guardia el único toque que tiene.</p>
   */
  disponible(): Observable<boolean> {
    return new Observable<boolean>(observador => {
      observador.next(false);
      observador.complete();
    });
  }

  /**
   * Pide UNA lectura al sensor. El enrolamiento la llama tres veces.
   *
   * <p>Falla mientras no haya lector, y falla en vez de quedarse esperando: una
   * promesa que nunca resuelve deja la pantalla colgada sin decir por qué.</p>
   */
  capturar(): Observable<LecturaHuella> {
    return throwError(() => new Error('No hay lector de huella configurado.'));
  }
}
