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

  /** Piso del selector: una visita no puede agendarse en el pasado. */
  readonly hoy = this.hoyIso();

  /**
   * Las horas que se pueden elegir, cada media hora.
   *
   * <p>El selector nativo de fecha-y-hora obligaba a buscar el minuto en una
   * rueda de sesenta valores. Nadie invita a alguien a las 19:49: en una visita
   * la media hora es toda la precisión que hace falta, y así la hora se elige
   * de una lista corta en vez de rodando.</p>
   *
   * <p>Se guarda en 24 h y se muestra en 12 h: el valor tiene que ordenarse y
   * compararse como texto, pero quien invita piensa en "7 de la noche".</p>
   *
   * <p>23:59 va al final porque no es una hora a la que se cite a nadie: es
   * "hasta que termine el día", y es el fin por defecto.</p>
   */
  readonly horas: { valor: string; etiqueta: string }[] = InvitadosComponent.construirHoras();

  /**
   * Casi toda visita es para hoy o para mañana. Ese caso no debería costar
   * abrir un calendario: se resuelve con un toque y el calendario queda para
   * la excepción, que es el fin de semana o el mes que viene.
   */
  readonly atajos: { etiqueta: string; fecha: string }[] = [
    { etiqueta: 'Hoy', fecha: this.diaIso(0) },
    { etiqueta: 'Mañana', fecha: this.diaIso(1) }
  ];

  // Sin tope de ingresos: la visita la acota su ventana. El invitado que sale
  // a comprar y vuelve gastaba su único cupo y se quedaba afuera.
  readonly formulario = this.fb.nonNullable.group({
    nombreInvitado: ['', [Validators.required]],
    documentoInvitado: ['', [Validators.required]],
    placa: [''],
    inicioFecha: [this.hoyIso(), [Validators.required]],
    inicioHora: [this.horaRedondeada(), [Validators.required]],
    finFecha: [this.hoyIso(), [Validators.required]],
    finHora: ['23:59', [Validators.required]]
  });

  /** Las dos mitades vueltas a juntar, que es lo que viaja y se compara. */
  get inicio(): string {
    const v = this.formulario.getRawValue();
    return `${v.inicioFecha}T${v.inicioHora}`;
  }

  get fin(): string {
    const v = this.formulario.getRawValue();
    return `${v.finFecha}T${v.finHora}`;
  }

  get inicioEnPasado(): boolean {
    const campo = this.formulario.controls.inicioFecha;
    return campo.touched && this.inicio < this.ahoraIso();
  }

  get finAntesDelInicio(): boolean {
    const fin = this.formulario.controls.finFecha;
    return (fin.touched || this.formulario.controls.finHora.touched)
      && this.fin <= this.inicio;
  }

  /**
   * Al correr el inicio, el fin lo sigue hasta el final de ESE día. Quien
   * invita para el sábado no tiene que corregir dos campos para decir una sola
   * cosa, y un fin que quedó antes del inicio nunca llega al servidor.
   */
  alCambiarInicio(): void {
    if (this.fin <= this.inicio) {
      this.formulario.controls.finFecha.setValue(this.formulario.controls.inicioFecha.value);
      this.formulario.controls.finHora.setValue('23:59');
    }
  }

  usarAtajo(atajo: { fecha: string }): void {
    this.formulario.controls.inicioFecha.setValue(atajo.fecha);
    this.alCambiarInicio();
  }

  /** 00:00, 00:30, … 23:30, y 23:59 al final como "fin del día". */
  private static construirHoras(): { valor: string; etiqueta: string }[] {
    const lista: { valor: string; etiqueta: string }[] = [];
    for (let minutos = 0; minutos < 24 * 60; minutos += 30) {
      lista.push(InvitadosComponent.hora(minutos));
    }
    lista.push(InvitadosComponent.hora(23 * 60 + 59));
    return lista;
  }

  /** 1170 → `{ valor: '19:30', etiqueta: '7:30 p.m.' }`. */
  private static hora(minutosDelDia: number): { valor: string; etiqueta: string } {
    const h = Math.floor(minutosDelDia / 60);
    const m = minutosDelDia % 60;
    // Las 12 no son las 0: mediodía y medianoche se escriben igual y solo las
    // distingue el sufijo.
    const doce = h % 12 === 0 ? 12 : h % 12;
    return {
      valor: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`,
      etiqueta: `${doce}:${String(m).padStart(2, '0')} ${h < 12 ? 'a.m.' : 'p.m.'}`
    };
  }

  /**
   * La media hora en curso, para que el inicio por defecto sea una hora que
   * SÍ está en la lista. Con 19:49 el select no tendría nada seleccionado.
   */
  private horaRedondeada(): string {
    const ahora = new Date();
    const hh = String(ahora.getHours()).padStart(2, '0');
    return `${hh}:${ahora.getMinutes() < 30 ? '00' : '30'}`;
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
    if (this.fin <= this.inicio) {
      this.formulario.controls.finFecha.markAsTouched();
      return;
    }
    this.guardando = true;
    this.error = null;

    // "Ya" se manda como null y no como el instante que marcaba el reloj cuando
    // se abrió el formulario: entre abrirlo y guardarlo pasan minutos, y ese
    // inicio ya vencido nacería en el pasado. El backend lo lee como "desde
    // ahora". Las horas van CON el offset local: sin él, el servidor las leería
    // como UTC y la invitación moriría horas antes de lo que dice la pantalla.
    const empiezaYa = this.inicio <= this.ahoraIso();
    const desde = empiezaYa ? null : this.conOffsetLocal(this.inicio);

    this.residente
      .crearInvitacion({
        nombreInvitado: datos.nombreInvitado,
        documentoInvitado: datos.documentoInvitado,
        placa: datos.placa || null,
        vigenciaDesde: desde,
        vigenciaHasta: this.conOffsetLocal(this.fin)
      })
      .subscribe({
        next: creada => {
          this.guardando = false;
          this.mostrarAlta = false;
          this.formulario.reset({
            inicioFecha: this.hoyIso(),
            inicioHora: this.horaRedondeada(),
            finFecha: this.hoyIso(),
            finHora: '23:59'
          });
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

  /**
   * El menú del sistema —WhatsApp, mensajes, correo— solo existe en el
   * teléfono. En un escritorio no hay nada que abrir, así que ahí el botón no
   * se dibuja y copiar pasa a ser la acción principal.
   */
  readonly puedeCompartir = typeof navigator !== 'undefined' && !!navigator.share;

  linkDe(invitacion: Invitacion): string {
    return `${location.origin}/invitado/${invitacion.codigoPublico}`;
  }

  private mensajeDe(invitacion: Invitacion): string {
    return `Hola ${invitacion.nombreInvitado}, te comparto tu código de ingreso `
      + `al conjunto (casa ${invitacion.casaIdentificador}): ${this.linkDe(invitacion)}`;
  }

  compartir(invitacion: Invitacion): void {
    navigator
      .share({ title: 'Invitación de ingreso', text: this.mensajeDe(invitacion) })
      // Cancelar el menú del sistema lanza AbortError. No es un fallo: es la
      // persona diciendo "ahora no", y no merece un banner rojo.
      .catch(() => undefined);
  }

  copiar(invitacion: Invitacion): void {
    const mensaje = this.mensajeDe(invitacion);

    navigator.clipboard.writeText(mensaje).then(
      () => this.confirmarCopia(),
      // Sin permiso de portapapeles —o sin HTTPS, que es el caso al probar
      // desde otro dispositivo en la red local— queda el respaldo de toda la
      // vida. Sin él, el botón no haría absolutamente nada y nadie sabría por qué.
      () => this.copiarALaAntigua(mensaje)
    );
  }

  private copiarALaAntigua(mensaje: string): void {
    const area = document.createElement('textarea');
    area.value = mensaje;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();

    const copiado = document.execCommand('copy');
    document.body.removeChild(area);

    if (copiado) {
      this.confirmarCopia();
    } else {
      this.error = 'No pudimos copiar el link. Mantén presionado el código para copiarlo.';
    }
  }

  private confirmarCopia(): void {
    this.aviso = 'Link copiado. Pégalo en WhatsApp o donde prefieras.';
    setTimeout(() => (this.aviso = null), 4000);
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

  // La ventana de la visita se pinta con el pipe rangoFechas: lo comparten
  // esta pantalla, la página del invitado y la tabla del administrador.

  /** Ahora mismo, como `YYYY-MM-DDTHH:mm`, para comparar contra el formulario. */
  private ahoraIso(): string {
    return this.aValorLocal(new Date());
  }

  /** Hoy, en el formato que entiende un `<input type="date">`. */
  private hoyIso(): string {
    return this.aValorLocal(new Date()).slice(0, 10);
  }

  /** El día de hoy corrido `dias` días. */
  private diaIso(dias: number): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + dias);
    return this.aValorLocal(fecha).slice(0, 10);
  }

  private aValorLocal(momento: Date): string {
    const mes = String(momento.getMonth() + 1).padStart(2, '0');
    const dia = String(momento.getDate()).padStart(2, '0');
    const hh = String(momento.getHours()).padStart(2, '0');
    const mm = String(momento.getMinutes()).padStart(2, '0');
    return `${momento.getFullYear()}-${mes}-${dia}T${hh}:${mm}`;
  }

  /** `2026-08-01T23:59` → `2026-08-01T23:59:00-05:00` (offset real del dispositivo). */
  private conOffsetLocal(valorLocal: string): string {
    const minutos = -new Date(`${valorLocal.slice(0, 10)}T12:00:00`).getTimezoneOffset();
    const signo = minutos >= 0 ? '+' : '-';
    const absolutos = Math.abs(minutos);
    const hh = String(Math.floor(absolutos / 60)).padStart(2, '0');
    const mm = String(absolutos % 60).padStart(2, '0');
    return `${valorLocal}:00${signo}${hh}:${mm}`;
  }
}
