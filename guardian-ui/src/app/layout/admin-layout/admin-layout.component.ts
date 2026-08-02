import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { SedeService } from '../../core/services/sede.service';
import { TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Back-office del administrador: barra lateral fija con las secciones y el
 * contenido a la derecha, el patrón clásico de un panel de gestión.
 *
 * <p>Es también el panel que ve el super administrador cuando entra a una
 * sede — por eso lleva la banda de suplantación.</p>
 */
@Component({
  selector: 'gd-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
  standalone: false
})
export class AdminLayoutComponent {

  sesion: Sesion | null = null;
  saliendo = false;

  readonly secciones = [
    { ruta: 'resumen', etiqueta: 'Resumen', icono: 'pi-chart-bar' },
    { ruta: 'casas', etiqueta: 'Casas', icono: 'pi-building' },
    { ruta: 'personas', etiqueta: 'Personas', icono: 'pi-users' },
    { ruta: 'vehiculos', etiqueta: 'Vehículos', icono: 'pi-car' },
    // "Usuarios" ya no está: la cuenta de alguien se administra sobre la
    // persona, en Personas. Dos pantallas para el mismo ser humano eran la
    // causa de que una dijera "Activa" y la otra le negara la entrada.
    { ruta: 'invitaciones', etiqueta: 'Invitaciones', icono: 'pi-ticket' },
    { ruta: 'bitacora', etiqueta: 'Bitácora', icono: 'pi-history' }
  ];

  constructor(
    private readonly auth: AuthService,
    private readonly sedeService: SedeService,
    private readonly router: Router,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  get suplantando(): boolean {
    return this.auth.sedeSuplantada;
  }

  /** Devuelve al super administrador a la plataforma, con token nuevo. */
  salirDeSede(): void {
    if (this.saliendo) {
      return;
    }
    this.saliendo = true;
    this.sedeService.salir().subscribe({
      next: () => {
        this.saliendo = false;
        this.router.navigate(['/sedes']);
      },
      // Si el backend falla, la sesión sigue siendo la de la sede: mandarlo a
      // /sedes de todos modos lo dejaría con un token que ese panel rechaza.
      error: () => (this.saliendo = false)
    });
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
