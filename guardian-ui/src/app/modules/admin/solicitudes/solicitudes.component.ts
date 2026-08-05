import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';

import { AdminService } from '../../../core/services/admin.service';
import {
  SolicitudCasaAdmin,
  SolicitudVehiculoAdmin
} from '../../../core/models/admin.model';

/** Lo que se está por rechazar, sin importar de qué bandeja salió. */
interface Rechazo {
  tipo: 'CASA' | 'VEHICULO';
  id: number;
  titulo: string;
}

/**
 * Bandeja del administrador: todo lo que espera una decisión suya.
 *
 * <p>Las dos clases de solicitud viven en la misma pantalla a propósito. Son la
 * misma pregunta —¿autorizo esto?— y separarlas en dos menús obligaría al
 * administrador a acordarse de revisar dos sitios; el que se le olvide deja a
 * alguien esperando.</p>
 */
@Component({
  selector: 'gd-solicitudes',
  templateUrl: './solicitudes.component.html',
  styleUrl: './solicitudes.component.scss',
  standalone: false
})
export class SolicitudesComponent implements OnInit {

  casas: SolicitudCasaAdmin[] = [];
  vehiculos: SolicitudVehiculoAdmin[] = [];

  cargando = true;
  resolviendo = false;
  error: string | null = null;

  rechazando: Rechazo | null = null;
  motivo = '';

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  get vacia(): boolean {
    return this.casas.length === 0 && this.vehiculos.length === 0;
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;

    forkJoin({
      casas: this.admin.solicitudesCasa(),
      vehiculos: this.admin.solicitudesVehiculo()
    }).subscribe({
      next: ({ casas, vehiculos }) => {
        this.casas = casas;
        this.vehiculos = vehiculos;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las solicitudes.';
        this.cargando = false;
      }
    });
  }

  aprobarCasa(solicitud: SolicitudCasaAdmin): void {
    this.resolver(this.admin.aprobarSolicitudCasa(solicitud.id),
      'No pudimos aprobar la solicitud.');
  }

  aprobarVehiculo(solicitud: SolicitudVehiculoAdmin): void {
    this.resolver(this.admin.aprobarSolicitudVehiculo(solicitud.id),
      'No pudimos autorizar el vehículo.');
  }

  abrirRechazoCasa(solicitud: SolicitudCasaAdmin): void {
    this.rechazando = { tipo: 'CASA', id: solicitud.id, titulo: solicitud.nombreCompleto };
    this.motivo = '';
  }

  abrirRechazoVehiculo(solicitud: SolicitudVehiculoAdmin): void {
    this.rechazando = { tipo: 'VEHICULO', id: solicitud.id, titulo: solicitud.placa };
    this.motivo = '';
  }

  confirmarRechazo(): void {
    const rechazo = this.rechazando;
    if (!rechazo) {
      return;
    }

    const peticion = rechazo.tipo === 'CASA'
      ? this.admin.rechazarSolicitudCasa(rechazo.id, this.motivo)
      : this.admin.rechazarSolicitudVehiculo(rechazo.id, this.motivo);

    this.resolver(peticion, 'No pudimos rechazar la solicitud.',
      () => (this.rechazando = null));
  }

  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Aprobar y rechazar terminan igual: recargando la bandeja entera. Entre
   * cargar y decidir pudo entrar otra solicitud, o la casa pudo conseguir
   * titular y cambiar lo que se puede aprobar — quitar la fila a mano dejaría
   * la pantalla mintiendo.
   */
  private resolver(peticion: Observable<unknown>, mensajeDeError: string,
                   alLograr?: () => void): void {
    if (this.resolviendo) {
      return;
    }
    this.resolviendo = true;
    this.error = null;

    peticion.subscribe({
      next: () => {
        this.resolviendo = false;
        if (alLograr) {
          alLograr();
        }
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? mensajeDeError;
        this.resolviendo = false;
        this.cargar();
      }
    });
  }
}
