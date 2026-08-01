import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResidenteService } from '../../../core/services/residente.service';
import { EstadoInvitacion, Invitacion } from '../../../core/models/acceso.model';

/**
 * Invitaciones de mi casa: QR de un solo uso (o pocos usos) que se comparte
 * por link. Solo revocar — el historial de ingresos pertenece al conjunto.
 */
@Component({
  selector: 'gd-invitados',
  templateUrl: './invitados.component.html',
  styleUrl: './invitados.component.scss',
  standalone: false
})
export class InvitadosComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  invitaciones: Invitacion[] = [];
  cargando = true;
  guardando = false;
  sinCasa = false;
  error: string | null = null;
  aviso: string | null = null;

  mostrarAlta = false;

  /** Invitación cuyo QR está desplegado en pantalla. */
  qrAbierto: Invitacion | null = null;

  /** Invitación pendiente de confirmar su revocación. */
  invitacionARevocar: Invitacion | null = null;

  /** Piso del selector de fecha: una visita no puede agendarse en el pasado. */
  readonly hoy = this.hoyIso();

  readonly formulario = this.fb.nonNullable.group({
    nombreInvitado: ['', [Validators.required]],
    documentoInvitado: ['', [Validators.required]],
    placa: [''],
    fechaVisita: [this.hoyIso(), [Validators.required]],
    usosMaximos: [1, [Validators.required, Validators.min(1), Validators.max(20)]]
  });

  get fechaEnPasado(): boolean {
    const campo = this.formulario.controls.fechaVisita;
    return campo.touched && campo.value < this.hoy;
  }

  constructor(private readonly residente: ResidenteService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.residente.invitaciones().subscribe({
      next: invitaciones => {
        this.invitaciones = invitaciones;
        this.cargando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        if (fallo.status === 400) {
          this.sinCasa = true;
        } else {
          this.error = fallo.error?.mensaje ?? 'No pudimos cargar tus invitaciones.';
        }
      }
    });
  }

  crear(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    const datos = this.formulario.getRawValue();
    if (datos.fechaVisita < this.hoyIso()) {
      this.formulario.controls.fechaVisita.markAsTouched();
      return;
    }
    this.guardando = true;
    this.error = null;

    // La visita es "ese día completo": desde las 00:00 si es una fecha futura
    // (o ahora si es hoy) hasta la medianoche. Las horas van CON el offset
    // local: sin él, el backend las leería como UTC y la invitación moriría
    // horas antes de la medianoche real del conjunto.
    const esHoy = datos.fechaVisita === this.hoyIso();
    const desde = esHoy ? null : this.conOffsetLocal(datos.fechaVisita, '00:00:00');

    this.residente
      .crearInvitacion({
        nombreInvitado: datos.nombreInvitado,
        documentoInvitado: datos.documentoInvitado,
        placa: datos.placa || null,
        vigenciaDesde: desde,
        vigenciaHasta: this.conOffsetLocal(datos.fechaVisita, '23:59:59'),
        usosMaximos: datos.usosMaximos
      })
      .subscribe({
        next: creada => {
          this.guardando = false;
          this.mostrarAlta = false;
          this.formulario.reset({ fechaVisita: this.hoyIso(), usosMaximos: 1 });
          this.cargar();
          // Abrir el QR de una vez: crear y compartir son un solo gesto.
          this.qrAbierto = creada;
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos crear la invitación.';
        }
      });
  }

  revocar(invitacion: Invitacion): void {
    this.invitacionARevocar = invitacion;
  }

  confirmarRevocar(): void {
    const invitacion = this.invitacionARevocar;
    if (!invitacion) {
      return;
    }

    this.error = null;
    this.residente.revocarInvitacion(invitacion.id).subscribe({
      next: () => {
        // Las dos hojas se cierran: la de confirmar porque terminó, y la del
        // código porque muestra un QR que acaba de morir.
        this.invitacionARevocar = null;
        this.qrAbierto = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.invitacionARevocar = null;
        this.error = fallo.error?.mensaje ?? 'No pudimos revocar la invitación.';
      }
    });
  }

  // ── Compartir ────────────────────────────────────────────────────────────

  linkDe(invitacion: Invitacion): string {
    return `${location.origin}/invitado/${invitacion.codigoPublico}`;
  }

  compartir(invitacion: Invitacion): void {
    const link = this.linkDe(invitacion);
    const texto = `Hola ${invitacion.nombreInvitado}, te comparto tu código de ingreso `
      + `al conjunto (casa ${invitacion.casaIdentificador}): ${link}`;

    if (navigator.share) {
      navigator.share({ title: 'Invitación de ingreso', text: texto }).catch(() => undefined);
      return;
    }
    navigator.clipboard.writeText(texto).then(() => {
      this.aviso = 'Link copiado. Pégalo en WhatsApp o donde prefieras.';
      setTimeout(() => (this.aviso = null), 4000);
    });
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  etiquetaEstado(estado: EstadoInvitacion): string {
    switch (estado) {
      case 'VIGENTE': return 'Vigente';
      case 'NO_VIGENTE': return 'Aún no vigente';
      case 'AGOTADA': return 'Usada';
      case 'VENCIDA': return 'Vencida';
      case 'REVOCADA': return 'Revocada';
    }
  }

  esVigente(invitacion: Invitacion): boolean {
    return invitacion.estado === 'VIGENTE' || invitacion.estado === 'NO_VIGENTE';
  }

  private hoyIso(): string {
    const hoy = new Date();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${hoy.getFullYear()}-${mes}-${dia}`;
  }

  /** `2026-08-01` + `23:59:59` → `2026-08-01T23:59:59-05:00` (offset real del dispositivo). */
  private conOffsetLocal(fecha: string, hora: string): string {
    const minutos = -new Date(`${fecha}T12:00:00`).getTimezoneOffset();
    const signo = minutos >= 0 ? '+' : '-';
    const absolutos = Math.abs(minutos);
    const hh = String(Math.floor(absolutos / 60)).padStart(2, '0');
    const mm = String(absolutos % 60).padStart(2, '0');
    return `${fecha}T${hora}${signo}${hh}:${mm}`;
  }
}
