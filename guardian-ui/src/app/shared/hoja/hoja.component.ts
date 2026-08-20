import {
  booleanAttribute,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output
} from '@angular/core';

/**
 * Hoja que sube desde abajo — el patrón nativo que en el móvil reemplaza al
 * diálogo centrado del escritorio.
 *
 * <p>Por dentro es un <code>&lt;dialog&gt;</code> abierto con
 * <code>showModal()</code>, o sea que vive en la CAPA SUPERIOR del navegador:
 * se dibuja encima de toda la página y su marco de referencia es la pantalla,
 * no su padre en el árbol. Esa decisión no es un detalle de implementación —
 * es la razón de que el botón de guardar no pueda volver a quedar tapado ni
 * fuera de la vista, pase lo que pase con el CSS de quien la contenga.</p>
 *
 * <p>Uso: contenido por proyección normal y acciones en el slot
 * <code>[pie]</code>, que queda pegado abajo mientras el cuerpo se desplaza.</p>
 */
@Component({
  selector: 'gd-hoja',
  templateUrl: './hoja.component.html',
  styleUrl: './hoja.component.scss',
  standalone: false
})
export class HojaComponent implements OnChanges, OnDestroy {

  @Input() abierta = false;
  @Input() titulo = '';

  /** Contexto bajo el título: la sede, la casa, a quién pertenece lo que se edita. */
  @Input() subtitulo = '';

  /**
   * Deja la hoja a la altura de su contenido en vez de ocupar la pantalla
   * entera. Es para preguntas de sí-o-no, como {@code gd-confirmar}: abrir una
   * pantalla completa para decir "Cancelar / Eliminar" desorienta más que
   * ayuda. Todo lo que sea un FORMULARIO va a pantalla completa, que es donde
   * el pie tiene su sitio garantizado.
   */
  @Input({ transform: booleanAttribute }) compacta = false;

  @Output() cerrar = new EventEmitter<void>();

  /**
   * Contador y NO un booleano: dos hojas pueden solaparse (ver el código de
   * una invitación y encima confirmar su revocación). Con un booleano, cerrar
   * la segunda restauraría el scroll del fondo mientras la primera sigue
   * abierta, o lo dejaría bloqueado para siempre.
   */
  private static abiertas = 0;
  private static overflowPrevio = '';

  /** Lo que esta instancia aportó al contador, para poder devolverlo. */
  private contabilizada = false;

  /** A dónde devolver el foco al cerrar: al control que abrió la hoja. */
  private focoPrevio: HTMLElement | null = null;

  // ── El teclado de iOS ────────────────────────────────────────────────────
  //
  // En Safari de iOS la ventana NO se encoge cuando sube el teclado: se dibuja
  // ENCIMA. Sin medirlo, el pie con Guardar queda detrás — y no hay forma de
  // alcanzarlo, porque la hoja tampoco se desplaza: se desplaza su cuerpo.
  //
  // visualViewport es la única API que sabe cuánto ocupa. Se mide y se publica
  // en una variable CSS que la hoja usa para encogerse por abajo.
  //
  // No es exclusivo de iOS: Android tiene el mismo problema cuando el navegador
  // no redimensiona. Medir sirve en los dos.

  /** Cuánto del alto se lleva el teclado ahora mismo, en píxeles. */
  private alturaTeclado = 0;

  /** Alto visible en el momento de abrir, con el teclado todavía cerrado. */
  private alturaBase = 0;

  private readonly medirTeclado = (): void => {
    const vv = window.visualViewport;
    if (!vv) {
      return;
    }
    // Contra el alto que había AL ABRIR, no contra window.innerHeight: en
    // Safari de iOS innerHeight es el viewport grande —el que habría si las
    // barras del navegador se escondieran—, así que restarle vv.height daba
    // ~90px de "teclado" con el teclado cerrado y la hoja vivía encogida por
    // abajo. La diferencia contra la apertura sí es el teclado.
    const alto = Math.max(0, Math.round(this.alturaBase - vv.height));

    if (alto === this.alturaTeclado) {
      return;
    }
    this.alturaTeclado = alto;
    document.documentElement.style.setProperty('--teclado-alto', alto + 'px');
  };

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  ngOnChanges(): void {
    if (this.abierta) {
      this.abrir();
    } else {
      this.liberar();
    }
  }

  ngOnDestroy(): void {
    this.liberar();
  }

  /**
   * Cerrar tocando fuera, que en escritorio es lo que se espera de un diálogo.
   * Se compara contra el RECTÁNGULO y no contra `event.target`: el relleno de
   * la propia hoja también reporta al diálogo como destino, así que un clic en
   * el margen de arriba la habría cerrado con el formulario a medio llenar.
   *
   * En el móvil no aplica: ahí la hoja ocupa la pantalla y no hay "fuera".
   */
  alClicFuera(evento: MouseEvent): void {
    const caja = this.dialogo()?.getBoundingClientRect();
    if (!caja) {
      return;
    }
    const fuera = evento.clientX < caja.left || evento.clientX > caja.right
               || evento.clientY < caja.top || evento.clientY > caja.bottom;
    if (fuera) {
      this.cerrar.emit();
    }
  }

  private dialogo(): HTMLDialogElement | null {
    return this.host.nativeElement.querySelector('dialog');
  }

  private abrir(): void {
    if (this.contabilizada) {
      return;
    }
    if (HojaComponent.abiertas === 0) {
      HojaComponent.overflowPrevio = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
    }
    HojaComponent.abiertas++;
    this.contabilizada = true;

    this.alturaBase = window.visualViewport?.height ?? 0;
    window.visualViewport?.addEventListener('resize', this.medirTeclado);
    window.visualViewport?.addEventListener('scroll', this.medirTeclado);
    this.medirTeclado();

    this.focoPrevio = document.activeElement as HTMLElement | null;

    // Tras el render: el *ngIf todavía no creó el <dialog> en ngOnChanges, y el
    // contenido proyectado tampoco existe.
    setTimeout(() => {
      const dlg = this.dialogo();
      if (!dlg) {
        return;
      }
      try {
        if (typeof dlg.showModal !== 'function') {
          throw new Error('sin dialogos nativos');
        }
        if (!dlg.open) {
          // showModal y NO show: solo el modal entra en la capa superior, y solo
          // el modal vuelve inerte al resto de la página. Es lo que hace que el
          // foco y Escape funcionen sin código nuestro.
          dlg.showModal();
        }
      } catch {
        // Navegador sin diálogos nativos, o el elemento todavía no está en el
        // documento. Queda visible por CSS, sin capa superior — el
        // comportamiento que la aplicación tenía antes. Vale más una hoja sin
        // capa superior que ninguna hoja.
        dlg.setAttribute('open', '');
      }

      // El primero SIN data-no-autofoco: la X del encabezado es focoable para
      // el tabulador, pero abrir un formulario con el foco en "cerrar" no sirve.
      const candidatos = this.focoables();
      const destino = candidatos.find(el => !el.hasAttribute('data-no-autofoco'));
      (destino ?? candidatos[0])?.focus();
    });
  }

  private liberar(): void {
    if (!this.contabilizada) {
      return;
    }
    HojaComponent.abiertas--;
    this.contabilizada = false;

    // Antes de que el *ngIf lo saque del árbol: un diálogo que se elimina
    // abierto deja el resto de la página inerte en algunos navegadores.
    const dlg = this.dialogo();
    if (dlg?.open) {
      dlg.close();
    }

    window.visualViewport?.removeEventListener('resize', this.medirTeclado);
    window.visualViewport?.removeEventListener('scroll', this.medirTeclado);

    if (HojaComponent.abiertas === 0) {
      document.body.style.overflow = HojaComponent.overflowPrevio;
      // Se limpia solo cuando NO queda ninguna hoja: con dos superpuestas, la
      // que se cierra no puede borrarle la medida a la que sigue abierta.
      document.documentElement.style.removeProperty('--teclado-alto');
      this.alturaTeclado = 0;
    }

    this.focoPrevio?.focus();
    this.focoPrevio = null;
  }

  private focoables(): HTMLElement[] {
    return Array.from(this.host.nativeElement.querySelectorAll<HTMLElement>(
      'button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    )).filter(el => !el.hasAttribute('disabled') && el.offsetParent !== null);
  }
}
