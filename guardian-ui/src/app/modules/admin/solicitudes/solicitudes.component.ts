import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';

import { AdminService } from '../../../core/services/admin.service';
import {
  InvitacionPendienteAdmin,
  SolicitudCasaAdmin,
  SolicitudHogarAdmin,
  SolicitudVehiculoAdmin
} from '../../../core/models/admin.model';

/** Lo que se está por rechazar, sin importar de qué bandeja salió. */
interface Rechazo {
  tipo: 'CASA' | 'VEHICULO' | 'HOGAR' | 'INVITACION';
  id: number;
  titulo: string;
}

/**
 * Bandeja del administrador: todo lo que espera una decisión suya.
 *
 * <p>Las cuatro clases de solicitud viven en la misma pantalla a propósito. Son
 * la misma pregunta —¿autorizo esto?— y separarlas en varios menús obligaría al
 * administrador a acordarse de revisar cada uno; el que se le olvide deja a
 * alguien esperando en la puerta.</p>
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
  hogar: SolicitudHogarAdmin[] = [];
  invitaciones: InvitacionPendienteAdmin[] = [];

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
    return this.casas.length === 0 && this.vehiculos.length === 0
      && this.hogar.length === 0 && this.invitaciones.length === 0;
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;

    forkJoin({
      casas: this.admin.solicitudesCasa(),
      vehiculos: this.admin.solicitudesVehiculo(),
      hogar: this.admin.solicitudesHogar(),
      invitaciones: this.admin.invitacionesPendientes()
    }).subscribe({
      next: ({ casas, vehiculos, hogar, invitaciones }) => {
        this.casas = casas;
        this.vehiculos = vehiculos;
        this.hogar = hogar;
        this.invitaciones = invitaciones;
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

  aprobarHogar(solicitud: SolicitudHogarAdmin): void {
    this.resolver(this.admin.aprobarSolicitudHogar(solicitud.id),
      'No pudimos aprobar la solicitud.');
  }

  aprobarInvitacion(invitacion: InvitacionPendienteAdmin): void {
    this.resolver(this.admin.aprobarInvitacion(invitacion.id),
      'No pudimos aprobar la invitación.');
  }

  abrirRechazoCasa(solicitud: SolicitudCasaAdmin): void {
    this.rechazando = { tipo: 'CASA', id: solicitud.id, titulo: solicitud.nombreCompleto };
    this.motivo = '';
  }

  abrirRechazoVehiculo(solicitud: SolicitudVehiculoAdmin): void {
    this.rechazando = { tipo: 'VEHICULO', id: solicitud.id, titulo: solicitud.placa };
    this.motivo = '';
  }

  abrirRechazoHogar(solicitud: SolicitudHogarAdmin): void {
    this.rechazando = { tipo: 'HOGAR', id: solicitud.id, titulo: solicitud.nombreCompleto };
    this.motivo = '';
  }

  abrirRechazoInvitacion(invitacion: InvitacionPendienteAdmin): void {
    this.rechazando = { tipo: 'INVITACION', id: invitacion.id, titulo: invitacion.nombreInvitado };
    this.motivo = '';
  }

  confirmarRechazo(): void {
    const rechazo = this.rechazando;
    if (!rechazo) {
      return;
    }

    const peticiones: Record<Rechazo['tipo'], () => Observable<unknown>> = {
      CASA: () => this.admin.rechazarSolicitudCasa(rechazo.id, this.motivo),
      VEHICULO: () => this.admin.rechazarSolicitudVehiculo(rechazo.id, this.motivo),
      HOGAR: () => this.admin.rechazarSolicitudHogar(rechazo.id, this.motivo),
      INVITACION: () => this.admin.rechazarInvitacion(rechazo.id, this.motivo)
    };

    this.resolver(peticiones[rechazo.tipo](), 'No pudimos rechazar la solicitud.',
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
