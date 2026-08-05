import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { SolicitudCasaAdmin } from '../../../core/models/admin.model';

/**
 * Bandeja de solicitudes de casa.
 *
 * <p>Aprobar es lo que de verdad mete a alguien en un hogar —con sus
 * vehículos, sus invitaciones y su familia detrás—, así que la decisión es del
 * administrador y no de quien la pide.</p>
 */
@Component({
  selector: 'gd-solicitudes',
  templateUrl: './solicitudes.component.html',
  styleUrl: './solicitudes.component.scss',
  standalone: false
})
export class SolicitudesComponent implements OnInit {

  solicitudes: SolicitudCasaAdmin[] = [];
  cargando = true;
  resolviendo = false;
  error: string | null = null;

  /** Solicitud a la que se le va a escribir el motivo del rechazo. */
  rechazando: SolicitudCasaAdmin | null = null;
  motivo = '';

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;
    this.admin.solicitudesCasa().subscribe({
      next: solicitudes => {
        this.solicitudes = solicitudes;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las solicitudes.';
        this.cargando = false;
      }
    });
  }

  aprobar(solicitud: SolicitudCasaAdmin): void {
    if (this.resolviendo) {
      return;
    }
    this.resolviendo = true;
    this.error = null;

    this.admin.aprobarSolicitudCasa(solicitud.id).subscribe({
      next: () => {
        this.resolviendo = false;
        // Se recarga la bandeja entera y no se quita la fila a mano: entre
        // cargar y aprobar pudo entrar otra solicitud, o la casa pudo
        // conseguir titular y cambiar lo que se puede aprobar.
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos aprobar la solicitud.';
        this.resolviendo = false;
        this.cargar();
      }
    });
  }

  abrirRechazo(solicitud: SolicitudCasaAdmin): void {
    this.rechazando = solicitud;
    this.motivo = '';
  }

  confirmarRechazo(): void {
    const solicitud = this.rechazando;
    if (!solicitud || this.resolviendo) {
      return;
    }
    this.resolviendo = true;
    this.error = null;

    this.admin.rechazarSolicitudCasa(solicitud.id, this.motivo).subscribe({
      next: () => {
        this.rechazando = null;
        this.resolviendo = false;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos rechazar la solicitud.';
        this.resolviendo = false;
      }
    });
  }
}
