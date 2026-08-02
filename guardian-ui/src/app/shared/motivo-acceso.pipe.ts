import { Pipe, PipeTransform } from '@angular/core';

import { Parametro } from '../core/models/admin.model';

/**
 * Traduce el código de denegación al texto del catálogo.
 *
 * <p>Recibe el catálogo como argumento en vez de pedirlo por HTTP: así el pipe
 * sigue siendo puro y el texto sale de <code>GD_PARAMETRO</code>, que es donde
 * vive. Un mapa hardcodeado acá se desincronizaría con el backend el día que se
 * agregue un motivo nuevo, y la bitácora volvería a mostrar el código crudo.</p>
 */
@Pipe({ name: 'motivoAcceso', standalone: false })
export class MotivoAccesoPipe implements PipeTransform {

  transform(codigo: string | null | undefined, catalogo: Parametro[] | null): string {
    if (!codigo) {
      return 'Denegado';
    }
    const opcion = catalogo?.find(p => p.codigo === codigo);
    if (opcion) {
      return opcion.valor;
    }
    // Un motivo que el catálogo todavía no conoce se muestra legible en vez
    // de en mayúsculas con guiones bajos.
    return codigo.charAt(0) + codigo.slice(1).toLowerCase().replace(/_/g, ' ');
  }
}
