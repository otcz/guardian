import { Component } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';

/**
 * La salida hacia el panel propio del usuario.
 *
 * <p>Un solo componente para las tres pantallas donde alguien puede entrar de
 * paso —portería, Mi cuenta y el cambio de PIN—: si cada layout se dibuja su
 * propio enlace, terminan con tres formas y tres sitios distintos, que es
 * justo lo que hace que una aplicación se sienta desordenada.</p>
 *
 * <p>Sin entradas a propósito. Sabe solo con la sesión y la URL si quien mira
 * está en su casa o de visita, así que ponerlo en una cabecera es una línea y
 * no hay forma de configurarlo mal.</p>
 */
@Component({
  selector: 'gd-volver',
  templateUrl: './volver.component.html',
  styleUrl: './volver.component.scss',
  standalone: false
})
export class VolverComponent {

  constructor(private readonly auth: AuthService) {}

  /** Nada que ofrecer cuando el usuario ya está en el panel que le toca. */
  get visible(): boolean {
    return this.auth.deVisita;
  }

  get destino(): string {
    return this.auth.rutaInicial();
  }

  /**
   * El nombre del destino y no un "←" pelado: una flecha sola obliga a
   * adivinar a dónde lleva, y quien entró a la portería desde el panel de
   * administración necesita ver el camino de vuelta sin abrirlo.
   */
  get nombre(): string {
    return this.auth.nombrePanelInicial();
  }
}
