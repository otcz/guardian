import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Consola de la portería. Sin navegación que distraiga: el guardia entra a
 * escanear y no necesita nada más. Los accesos a otros paneles quedan como
 * iconos discretos en la esquina.
 */
@Component({
  selector: 'gd-porteria-layout',
  templateUrl: './porteria-layout.component.html',
  styleUrl: './porteria-layout.component.scss',
  standalone: false
})
export class PorteriaLayoutComponent {

  sesion: Sesion | null = null;

  constructor(
    private readonly auth: AuthService,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
