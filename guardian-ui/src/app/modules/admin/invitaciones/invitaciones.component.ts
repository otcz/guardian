import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { EstadoInvitacion, Invitacion } from '../../../core/models/acceso.model';

/**
 * Todas las invitaciones del conjunto. El administrador puede revocar
 * cualquiera — es su palanca ante un QR de invitado que circula de más.
 */
@Component({
  selector: 'gd-admin-invitaciones',
  templateUrl: './invitaciones.component.html',
  styleUrl: './invitaciones.component.scss',
  standalone: false
})
export class InvitacionesComponent implements OnInit {

  invitaciones: Invitacion[] = [];
  cargando = true;
  error: string | null = null;

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.admin.invitaciones().subscribe({
      next: invitaciones => {
        this.invitaciones = invitaciones;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las invitaciones.';
        this.cargando = false;
      }
    });
  }

  revocar(invitacion: Invitacion): void {
    const seguro = window.confirm(
      `¿Revocar la invitación de ${invitacion.nombreInvitado} `
      + `(casa ${invitacion.casaIdentificador})? El código dejará de servir.`);
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.revocarInvitacion(invitacion.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos revocar la invitación.';
      }
    });
  }

  etiquetaEstado(estado: EstadoInvitacion): string {
    switch (estado) {
      case 'VIGENTE': return 'Vigente';
      case 'NO_VIGENTE': return 'Aún no vigente';
      case 'AGOTADA': return 'Usada';
      case 'VENCIDA': return 'Vencida';
      case 'REVOCADA': return 'Revocada';
    }
  }

  revocable(invitacion: Invitacion): boolean {
    return invitacion.estado === 'VIGENTE' || invitacion.estado === 'NO_VIGENTE';
  }
}
