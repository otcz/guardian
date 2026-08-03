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

  /**
   * El guardia vive acá; el administrador está de paso. Solo al segundo se le
   * ofrece volver, y con el nombre del panel — un engranaje sin etiqueta
   * obliga a adivinar a dónde lleva.
   */
  get deVisita(): boolean {
    return this.auth.estaDeVisitaEn('/porteria');
  }

  get panelPropio(): string {
    return this.auth.rutaInicial();
  }

  get nombrePanelPropio(): string {
    return this.auth.nombrePanelInicial();
  }


  get puedeIrAAdmin(): boolean {
    return this.auth.tieneRol('ADMIN');
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
