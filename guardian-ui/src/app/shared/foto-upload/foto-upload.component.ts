import { Component, EventEmitter, Input, Output } from '@angular/core';

import { FotoService } from '../../core/services/foto.service';

/**
 * Selector + carga de foto de persona. Sube el archivo apenas se elige y
 * emite la URL que el formulario padre guarda en fotoUrl.
 */
@Component({
  selector: 'gd-foto-upload',
  templateUrl: './foto-upload.component.html',
  styleUrl: './foto-upload.component.scss',
  standalone: false
})
export class FotoUploadComponent {

  @Input() fotoUrl: string | null = null;
  @Output() fotoUrlChange = new EventEmitter<string | null>();

  cargando = false;
  error: string | null = null;

  constructor(private readonly fotoService: FotoService) {}

  get vistaPrevia(): string | null {
    return this.fotoService.urlAbsoluta(this.fotoUrl);
  }

  alSeleccionar(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) {
      return;
    }

    this.cargando = true;
    this.error = null;

    this.fotoService.subir(archivo).subscribe({
      next: url => {
        this.cargando = false;
        this.fotoUrl = url;
        this.fotoUrlChange.emit(url);
      },
      error: fallo => {
        this.cargando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos subir la foto.';
      }
    });

    // Permite volver a elegir el mismo archivo si el usuario se arrepiente.
    input.value = '';
  }

  quitar(): void {
    this.fotoUrl = null;
    this.fotoUrlChange.emit(null);
  }
}
