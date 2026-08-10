import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { FiltroTabla } from '../../../shared/tabla/filtro-tabla';
import { AuthService } from '../../../core/services/auth.service';
import { EstadoInvitacion, Invitacion } from '../../../core/models/acceso.model';

/**
 * Todas las invitaciones del conjunto.
 *
 * <p>Copiar el link y revocar, porque la administración es quien atiende
 * cuando el residente no está. Revocar es su palanca ante un QR que circula
 * de más.</p>
 *
 * <p>Eliminar solo lo ve el super administrador: una invitación borrada es un
 * hueco en la auditoría — alguien pudo entrar por ese código y no quedaría
 * registro de quién lo emitió.</p>
 */
@Component({
  selector: 'gd-admin-invitaciones',
  templateUrl: './invitaciones.component.html',
  styleUrl: './invitaciones.component.scss',
  standalone: false
})
export class InvitacionesComponent implements OnInit {

  private readonly auth = inject(AuthService);

  invitaciones: Invitacion[] = [];

  /** Lo que la tabla pinta: las invitaciones que pasan el autofiltro. */
  visibles: Invitacion[] = [];

  /**
   * Autofiltro por columna. Sin filtro por invitado ni documento —cada valor
   * dejaría una sola fila— y con uno sobre la placa vuelto pregunta útil: si
   * el visitante llegó en carro o a pie.
   */
  readonly filtro = new FiltroTabla<Invitacion>(
    {
      casa: i => i.casaIdentificador,
      anfitrion: i => i.anfitrionNombre,
      llegada: i => (i.placa ? 'En vehículo' : 'A pie'),
      estado: i => this.etiquetaEstado(i.estado)
    },
    i => `${i.nombreInvitado} ${i.documentoInvitado} ${i.placa ?? ''} ${i.anfitrionNombre}`
  );
  cargando = true;
  error: string | null = null;
  aviso: string | null = null;

  /**
   * Borrar es del operador de la plataforma y de nadie más. El administrador
   * de la sede desactiva, bloquea y revoca — todo eso deja rastro. Borrar no:
   * la fila desaparece y con ella el registro que podría resolver una disputa.
   * El backend lo exige igual; esto solo evita ofrecer un botón que va a
   * responder 403.
   */
  readonly puedeEliminar = this.auth.tieneRol('SUPER_ADMIN');

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
        this.filtrar();
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

  /** Recalcula la lista visible. Se llama tras cargar y tras cada cambio. */
  filtrar(): void {
    this.visibles = this.filtro.aplicar(this.invitaciones);
  }

  etiquetaEstado(estado: EstadoInvitacion): string {
    switch (estado) {
      case 'PENDIENTE': return 'Pendiente de aprobación';
      case 'RECHAZADA': return 'Rechazada';
      case 'VIGENTE': return 'Vigente';
      case 'NO_VIGENTE': return 'Aún no vigente';
      case 'AGOTADA': return 'Usada';
      case 'VENCIDA': return 'Vencida';
      case 'REVOCADA': return 'Revocada';
    }
  }

  /**
   * PENDIENTE se trata como NO_VIGENTE: todavía no sirve en la portería, pero
   * el anfitrión sigue pudiendo compartir el link o revocarla de una vez.
   * RECHAZADA ya es un punto muerto, igual que VENCIDA o AGOTADA.
   */
  revocable(invitacion: Invitacion): boolean {
    return invitacion.estado === 'VIGENTE' || invitacion.estado === 'NO_VIGENTE'
      || invitacion.estado === 'PENDIENTE';
  }
}
