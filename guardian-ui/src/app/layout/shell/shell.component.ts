import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Marco de la aplicación: encabezado con la identidad y navegación por rol.
 */
@Component({
  selector: 'gd-shell',
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  standalone: false
})
export class ShellComponent {

  sesion: Sesion | null = null;
  modoOscuro = false;

  constructor(private readonly auth: AuthService) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
    this.modoOscuro = document.documentElement.classList.contains('dark-mode');
  }

  get esGuardia(): boolean {
    return this.auth.tieneRol('GUARDIA', 'ADMIN');
  }

  get esAdmin(): boolean {
    return this.auth.tieneRol('ADMIN');
  }

  alternarTema(): void {
    this.modoOscuro = !this.modoOscuro;
    document.documentElement.classList.toggle('dark-mode', this.modoOscuro);
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
