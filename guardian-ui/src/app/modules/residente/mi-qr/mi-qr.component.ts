import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AccesoService } from '../../../core/services/acceso.service';
import { LLAVE_CACHE_MI_QR as LLAVE_CACHE, MiQr } from '../../../core/models/acceso.model';

/**
 * La credencial del residente.
 *
 * <p>El QR se dibuja en el teléfono a partir del payload, y el payload se
 * guarda en el dispositivo. Eso es a propósito: la portería es justo donde peor
 * señal hay, y una credencial que solo funciona con datos móviles deja a la
 * persona parada en la entrada. Al ser un QR permanente y firmado, el payload
 * cacheado sigue siendo válido.</p>
 */
@Component({
  selector: 'gd-mi-qr',
  templateUrl: './mi-qr.component.html',
  styleUrl: './mi-qr.component.scss',
  standalone: false
})
export class MiQrComponent implements OnInit {

  credencial: MiQr | null = null;
  cargando = true;
  desdeCache = false;
  error: string | null = null;

  constructor(private readonly acceso: AccesoService) {}

  ngOnInit(): void {
    this.credencial = this.leerCache();

    this.acceso.miQr().subscribe({
      next: credencial => {
        this.credencial = credencial;
        this.desdeCache = false;
        this.cargando = false;
        localStorage.setItem(LLAVE_CACHE, JSON.stringify(credencial));
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        if (this.credencial) {
          // Hay copia local: la credencial se muestra igual y solo se avisa
          // que no se pudo confirmar contra el servidor.
          this.desdeCache = true;
        } else {
          this.error = fallo.error?.mensaje ?? 'No pudimos cargar tu código.';
        }
      }
    });
  }

  private leerCache(): MiQr | null {
    const crudo = localStorage.getItem(LLAVE_CACHE);
    if (!crudo) {
      return null;
    }
    try {
      return JSON.parse(crudo) as MiQr;
    } catch {
      localStorage.removeItem(LLAVE_CACHE);
      return null;
    }
  }
}
