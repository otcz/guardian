import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Back-office del administrador: barra lateral fija con las secciones y el
 * contenido a la derecha, el patrón clásico de un panel de gestión.
 */
@Component({
  selector: 'gd-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
  standalone: false
})
export class AdminLayoutComponent {

  sesion: Sesion | null = null;

  readonly secciones = [
    { ruta: 'resumen', etiqueta: 'Resumen', icono: 'pi-chart-bar' },
    { ruta: 'casas', etiqueta: 'Casas', icono: 'pi-building' },
    { ruta: 'personas', etiqueta: 'Personas', icono: 'pi-users' },
    { ruta: 'vehiculos', etiqueta: 'Vehículos', icono: 'pi-car' },
    { ruta: 'usuarios', etiqueta: 'Usuarios', icono: 'pi-key' },
    { ruta: 'bitacora', etiqueta: 'Bitácora', icono: 'pi-history' }
  ];

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
