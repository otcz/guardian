import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { PreferenciaTema, TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Panel del usuario: su credencial y su hogar.
 *
 * <p>Pensado para el celular del residente y con la estructura de una app
 * nativa: cabecera compacta arriba y barra de navegación abajo, donde llega
 * el pulgar sin recolocar la mano.</p>
 */
@Component({
  selector: 'gd-residente-layout',
  templateUrl: './residente-layout.component.html',
  styleUrl: './residente-layout.component.scss',
  standalone: false
})
export class ResidenteLayoutComponent {

  sesion: Sesion | null = null;
  mostrarCuenta = false;

  readonly opcionesTema: { valor: PreferenciaTema; nombre: string; icono: string }[] = [
    { valor: 'sistema', nombre: 'Como el sistema', icono: 'pi-mobile' },
    { valor: 'claro', nombre: 'Claro', icono: 'pi-sun' },
    { valor: 'oscuro', nombre: 'Oscuro', icono: 'pi-moon' }
  ];

  constructor(
    private readonly auth: AuthService,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  /** El nombre completo no cabe en una cabecera compacta; el de pila sí. */
  get primerNombre(): string {
    return this.sesion?.nombreCompleto?.trim().split(/\s+/)[0] ?? '';
  }

  get inicial(): string {
    return this.primerNombre.charAt(0).toUpperCase();
  }

  get saludo(): string {
    const hora = new Date().getHours();
    if (hora < 12) {
      return 'Buenos días';
    }
    return hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  }

  get puedeIrAPorteria(): boolean {
    return this.auth.tieneRol('GUARDIA', 'ADMIN');
  }

  get puedeIrAAdmin(): boolean {
    return this.auth.tieneRol('ADMIN');
  }

  salir(): void {
    this.mostrarCuenta = false;
    this.auth.cerrarSesion();
  }
}
