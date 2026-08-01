import { Pipe, PipeTransform } from '@angular/core';

/**
 * Presenta un número de documento con separadores de miles, como se lee en la
 * cédula física: 1020304050 → «1.020.304.050». Los documentos alfanuméricos
 * (pasaportes, el usuario ADMIN) pasan sin tocar.
 *
 * Uso: {{ persona.documento | documento }}
 */
@Pipe({ name: 'documento', standalone: false })
export class DocumentoPipe implements PipeTransform {

  transform(documento: string | null | undefined): string {
    if (!documento) {
      return '';
    }
    if (!/^\d+$/.test(documento)) {
      return documento;
    }
    return Number(documento).toLocaleString('es-CO');
  }
}
