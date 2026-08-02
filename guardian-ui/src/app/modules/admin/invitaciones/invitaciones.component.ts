import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { EstadoInvitacion, Invitacion } from '../../../core/models/acceso.model';

/**
 * Todas las invitaciones del conjunto.
 *
 * <p>Las mismas tres acciones que tiene el residente sobre las suyas, porque
 * la administración es quien atiende cuando el residente no está: copiar el
 * link, revocar —su palanca ante un QR que circula de más— y eliminar. La
 * bitácora no se toca con ninguna.</p>
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
  aviso: string | null = null;

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
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

  eliminar(invitacion: Invitacion): void {
    const seguro = window.confirm(
      `¿Eliminar la invitación de ${invitacion.nombreInvitado} `
      + `(casa ${invitacion.casaIdentificador})? La bitácora conservará los `
      + 'ingresos que permitió.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.eliminarInvitacion(invitacion.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos eliminar la invitación.';
      }
    });
  }

  /**
   * El administrador no tiene el teléfono del invitado, así que no hay nada
   * que "compartir": copia el link para pegarlo donde haga falta. Por eso el
   * botón dice copiar y no usa el diálogo del sistema.
   */
  compartir(invitacion: Invitacion): void {
    const link = `${location.origin}/invitado/${invitacion.codigoPublico}`;
    navigator.clipboard.writeText(link).then(() => {
      this.aviso = `Link de ${invitacion.nombreInvitado} copiado.`;
      setTimeout(() => (this.aviso = null), 4000);
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
