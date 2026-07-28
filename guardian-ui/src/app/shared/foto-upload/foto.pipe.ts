import { Pipe, PipeTransform } from '@angular/core';

import { FotoService } from '../../core/services/foto.service';

/**
 * Resuelve la ruta relativa de una foto contra el host del API activo.
 * Uso: <img [src]="persona.fotoUrl | foto">
 */
@Pipe({ name: 'foto', standalone: false })
export class FotoPipe implements PipeTransform {

  constructor(private readonly fotoService: FotoService) {}

  transform(fotoUrl: string | null | undefined): string | null {
    return this.fotoService.urlAbsoluta(fotoUrl ?? null);
  }
}
