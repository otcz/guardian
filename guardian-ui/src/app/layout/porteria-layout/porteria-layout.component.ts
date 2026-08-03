import { Component, OnInit } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import {
  PorteriaActivaService,
  PorteriaGarita
} from '../../core/services/porteria-activa.service';
import { TemaService } from '../../core/services/tema.service';
import { Sesion } from '../../core/models/sesion.model';

/**
 * Consola de la portería. Sin navegación que distraiga: el guardia entra a
 * escanear y no necesita nada más. Los accesos a otros paneles quedan como
 * iconos discretos en la esquina.
 *
 * <p>Acá vive también EN QUÉ portería está esta tablet. Va en el layout y no en
 * el escáner porque tiene que resolverse ANTES del primer escaneo: entre el
 * código y el veredicto no puede aparecer una pregunta.</p>
 */
@Component({
  selector: 'gd-porteria-layout',
  templateUrl: './porteria-layout.component.html',
  styleUrl: './porteria-layout.component.scss',
  standalone: false
})
export class PorteriaLayoutComponent implements OnInit {

  sesion: Sesion | null = null;

  opciones: PorteriaGarita[] = [];
  porteria: PorteriaGarita | null = null;

  /** Hoja de elección abierta. Bloquea la consola cuando no hay ninguna fijada. */
  eligiendo = false;
  cargandoPorterias = true;
  errorPorterias = false;

  constructor(
    private readonly auth: AuthService,
    private readonly porterias: PorteriaActivaService,
    public readonly tema: TemaService
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  ngOnInit(): void {
    this.resolverPorteria();
  }

  resolverPorteria(): void {
    this.cargandoPorterias = true;
    this.errorPorterias = false;

    this.porterias.resolver(this.sesion?.sedeId ?? null).subscribe({
      next: resolucion => {
        this.opciones = resolucion.opciones;
        this.porteria = resolucion.elegida;
        this.eligiendo = resolucion.preguntar;
        this.cargandoPorterias = false;
      },
      error: () => {
        // Sin red no se deja al guardia encerrado: sigue operando con lo que la
        // tablet tenga guardado, y el backend cae a su propio respaldo si el id
        // ya no sirve. Una puerta bloqueada es peor que un dato incompleto.
        this.cargandoPorterias = false;
        this.errorPorterias = true;
      }
    });
  }

  elegir(porteria: PorteriaGarita): void {
    this.porterias.fijar(porteria, this.sesion?.sedeId ?? null);
    this.porteria = porteria;
    this.eligiendo = false;
  }

  /**
   * Cambiar de portería a mitad de turno. Se puede, pero no es un toque
   * suelto: con una sola opción no hay nada que elegir.
   */
  get puedeCambiar(): boolean {
    return this.opciones.length > 1;
  }

  abrirEleccion(): void {
    if (this.puedeCambiar) {
      this.eligiendo = true;
    }
  }

  /** Solo se puede cerrar la hoja si ya hay una portería fijada. */
  cerrarEleccion(): void {
    if (this.porteria) {
      this.eligiendo = false;
    }
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
