import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

import { AdminService } from '../../core/services/admin.service';
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
export class AdminLayoutComponent implements OnInit {

  sesion: Sesion | null = null;
  saliendo = false;

  /**
   * Solicitudes esperando respuesta: casas y vehículos, sumadas.
   *
   * <p>Va en el menú y no solo dentro de la pantalla: una solicitud sin
   * responder deja a alguien sin casa o con un carro que no entra al conjunto,
   * y si el administrador tiene que entrar a mirar para enterarse, nadie se
   * entera.</p>
   */
  solicitudesPendientes = 0;

  readonly secciones = [
    { ruta: 'resumen', etiqueta: 'Resumen', icono: 'pi-chart-bar' },
    { ruta: 'casas', etiqueta: 'Casas', icono: 'pi-building' },
    { ruta: 'personas', etiqueta: 'Personas', icono: 'pi-users' },
    { ruta: 'vehiculos', etiqueta: 'Vehículos', icono: 'pi-car' },
    // "Usuarios" ya no está: la cuenta de alguien se administra sobre la
    // persona, en Personas. Dos pantallas para el mismo ser humano eran la
    // causa de que una dijera "Activa" y la otra le negara la entrada.
    { ruta: 'invitaciones', etiqueta: 'Invitaciones', icono: 'pi-ticket' },
    // Con las pantallas del día a día y no abajo con la configuración: es una
    // bandeja, y una solicitud sin responder deja a alguien sin poder usar la
    // aplicación.
    { ruta: 'solicitudes', etiqueta: 'Solicitudes', icono: 'pi-inbox' },
    { ruta: 'bitacora', etiqueta: 'Bitácora', icono: 'pi-history' },
    // Con Configuración y no con Casas: las porterías se definen una vez, al
    // montar el conjunto, y después no se vuelven a tocar. Arriba le quitarían
    // sitio a las pantallas del día a día.
    { ruta: 'porterias', etiqueta: 'Porterías', icono: 'pi-shield' },
    // De último: es lo que menos se toca, y va después de las pantallas de
    // trabajo diario porque configura lo que aparece en ellas.
    { ruta: 'configuracion', etiqueta: 'Configuración', icono: 'pi-sliders-h' }
  ];

  constructor(
    private readonly auth: AuthService,
    private readonly sedeService: SedeService,
    private readonly admin: AdminService,
    private readonly router: Router,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));

    // Se recuenta en cada navegación del panel y no con un temporizador: el
    // administrador que acaba de aprobar una tiene que ver el número bajar al
    // volver, y un sondeo cada X segundos gastaría peticiones toda la jornada
    // para un dato que cambia unas pocas veces al día.
    this.router.events
      .pipe(filter(evento => evento instanceof NavigationEnd))
      .subscribe(() => this.contarSolicitudes());
  }

  /**
   * El cajón lateral en el teléfono. En escritorio no se usa: ahí la barra
   * está siempre a la vista.
   */
  menuAbierto = false;

  /* La constante se declara ANTES del campo que la usa: los estáticos se
     inicializan en orden y al revés queda indefinida al leerla. */
  private static readonly CLAVE_PLEGADA = 'guardian.menu-plegado';

  /**
   * Barra plegada a solo iconos, en escritorio. Se recuerda entre sesiones:
   * quien trabaja con la tabla ancha la pliega una vez, no en cada entrada.
   */
  plegada = localStorage.getItem(AdminLayoutComponent.CLAVE_PLEGADA) === 'S';

  alternarPlegado(): void {
    this.plegada = !this.plegada;
    localStorage.setItem(AdminLayoutComponent.CLAVE_PLEGADA, this.plegada ? 'S' : 'N');
  }

  ngOnInit(): void {
    this.contarSolicitudes();

    // Navegar cierra el cajón. Sin esto, tocar una sección deja el menú
    // abierto encima de la pantalla que se acaba de pedir.
    this.router.events
      .pipe(filter(evento => evento instanceof NavigationEnd))
      .subscribe(() => (this.menuAbierto = false));
  }

  private contarSolicitudes(): void {
    this.admin.solicitudesPendientes().subscribe({
      next: conteo => (this.solicitudesPendientes = conteo.pendientes),
      // Un aviso que no carga no puede romper el panel: se queda sin número.
      error: () => (this.solicitudesPendientes = 0)
    });
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
