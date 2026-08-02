import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ResidenteService } from '../../../core/services/residente.service';
import { EstadoInvitacion, Invitacion } from '../../../core/models/acceso.model';

/** Las dos acciones sin vuelta atrás sobre una invitación. */
type AccionDestructiva = 'revocar' | 'eliminar';

/**
 * Invitaciones de mi casa: QR de pocos usos que se comparte por link.
 *
 * <p>Tres acciones sobre cada una. Compartir es la del 90% de las veces.
 * Revocar mata el código de inmediato y deja la fila a la vista. Eliminar la
 * saca de la lista, que después de un mes invitando gente son cuarenta filas
 * vencidas y encontrar la de hoy cuesta más que crear una nueva. El historial
 * de ingresos no se va con ninguna de las dos: pertenece al conjunto.</p>
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

  /**
   * Lo que está pendiente de confirmar. Una sola hoja para las dos acciones
   * destructivas: son el mismo gesto y el mismo riesgo, cambia el texto.
   */
  confirmacion: { invitacion: Invitacion; accion: AccionDestructiva } | null = null;

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
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
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

  // ── Revocar y eliminar ───────────────────────────────────────────────────

  revocar(invitacion: Invitacion): void {
    this.confirmacion = { invitacion, accion: 'revocar' };
  }

  eliminar(invitacion: Invitacion): void {
    this.confirmacion = { invitacion, accion: 'eliminar' };
  }

  confirmar(): void {
    const pendiente = this.confirmacion;
    if (!pendiente) {
      return;
    }

    this.error = null;
    // `unknown` y no la unión de los dos tipos de respuesta: revocar devuelve
    // la invitación y eliminar no devuelve nada, y aquí no se usa ninguno de
    // los dos — después de cualquiera de las dos se recarga la lista.
    const peticion: Observable<unknown> = pendiente.accion === 'revocar'
      ? this.residente.revocarInvitacion(pendiente.invitacion.id)
      : this.residente.eliminarInvitacion(pendiente.invitacion.id);

    peticion.subscribe({
      next: () => {
        // Las dos hojas se cierran: la de confirmar porque terminó, y la del
        // código porque muestra un QR que acaba de morir.
        this.confirmacion = null;
        this.qrAbierto = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        const accion = pendiente.accion === 'revocar' ? 'revocar' : 'eliminar';
        this.confirmacion = null;
        this.error = fallo.error?.mensaje ?? `No pudimos ${accion} la invitación.`;
      }
    });
  }

  get tituloConfirmacion(): string {
    return this.confirmacion?.invitacion.nombreInvitado ?? '';
  }

  /** El aviso dice qué se pierde, que es lo único que hay que decidir. */
  get avisoConfirmacion(): string {
    if (this.confirmacion?.accion === 'revocar') {
      return 'El código dejará de servir de inmediato.';
    }
    return 'La invitación sale de tu lista. Los ingresos que ya permitió quedan '
      + 'en el registro del conjunto.';
  }

  get botonConfirmacion(): string {
    return this.confirmacion?.accion === 'revocar' ? 'Revocar invitación' : 'Eliminar invitación';
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
