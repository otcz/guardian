import { Component } from '@angular/core';

/**
 * Marco del panel de administración: subnavegación fija + contenido.
 */
@Component({
  selector: 'gd-admin-shell',
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
  standalone: false
})
export class AdminShellComponent {

  readonly secciones = [
    { ruta: 'casas', etiqueta: 'Casas', icono: 'pi-building' },
    { ruta: 'personas', etiqueta: 'Personas', icono: 'pi-users' },
    { ruta: 'vehiculos', etiqueta: 'Vehículos', icono: 'pi-car' },
    { ruta: 'usuarios', etiqueta: 'Usuarios', icono: 'pi-key' },
    { ruta: 'bitacora', etiqueta: 'Bitácora', icono: 'pi-history' }
  ];
}
