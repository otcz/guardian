import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Panel del usuario: su credencial y su hogar. Pensado para el celular del
 * residente — encabezado compacto y pestañas grandes.
 */
@Component({
  selector: 'gd-residente-layout',
  templateUrl: './residente-layout.component.html',
  styleUrl: './residente-layout.component.scss',
  standalone: false
})
export class ResidenteLayoutComponent {

  sesion: Sesion | null = null;

  constructor(
    private readonly auth: AuthService,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  get puedeIrAPorteria(): boolean {
    return this.auth.tieneRol('GUARDIA', 'ADMIN');
  }

  get puedeIrAAdmin(): boolean {
    return this.auth.tieneRol('ADMIN');
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
