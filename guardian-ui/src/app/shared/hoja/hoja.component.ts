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

  // ── El área visible de verdad ────────────────────────────────────────────
  //
  // En Safari de iOS la ventana NO se encoge cuando sube el teclado: se dibuja
  // ENCIMA. Y cuando el campo enfocado queda bajo, iOS ademas DESPLAZA la vista
  // — el borde visible ya no arranca en el 0 del marco de los elementos fijos.
  // Cualquier formula que "deduzca" el teclado a partir de un alto capturado al
  // abrir se desactualiza al rotar, al esconderse las barras del navegador o si
  // la hoja se abre con el teclado ya arriba. Todas esas variantes ya mordieron.
  //
  // Asi que no se deduce nada: se publica el visual viewport TAL CUAL lo
  // reporta el navegador — tope, alto y fondo — y la hoja se ancla a esa area.
  // Si el navegador mueve el area visible, la hoja la sigue; no hay estado
  // propio que pueda quedarse viejo.

  /** Ultimo valor publicado, para no tocar el DOM si nada cambio. */
  private vistaPublicada = '';

  private readonly publicarVista = (): void => {
    const vv = window.visualViewport;
    // Sin la API, el area visible es la ventana entera: top 0, fondo 0.
    const top = vv ? Math.max(0, Math.round(vv.offsetTop)) : 0;
    const alto = vv ? Math.round(vv.height) : window.innerHeight;
    const abajo = vv
        ? Math.max(0, Math.round(window.innerHeight - vv.offsetTop - vv.height))
        : 0;

    const firma = top + '/' + alto + '/' + abajo;
    if (firma === this.vistaPublicada) {
      return;
    }
    this.vistaPublicada = firma;

    const raiz = document.documentElement.style;
    raiz.setProperty('--vv-top', top + 'px');
    raiz.setProperty('--vv-alto', alto + 'px');
    raiz.setProperty('--vv-abajo', abajo + 'px');
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

    window.visualViewport?.addEventListener('resize', this.publicarVista);
    window.visualViewport?.addEventListener('scroll', this.publicarVista);
    // Sincrono, ANTES de que el *ngIf pinte el dialogo: las variables ya estan
    // en su sitio en el primer fotograma y la hoja nunca mide con un valor
    // viejo de otra apertura.
    this.publicarVista();

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

    window.visualViewport?.removeEventListener('resize', this.publicarVista);
    window.visualViewport?.removeEventListener('scroll', this.publicarVista);

    if (HojaComponent.abiertas === 0) {
      document.body.style.overflow = HojaComponent.overflowPrevio;
      // Se limpia solo cuando NO queda ninguna hoja: con dos superpuestas, la
      // que se cierra no puede borrarle la medida a la que sigue abierta.
      const raiz = document.documentElement.style;
      raiz.removeProperty('--vv-top');
      raiz.removeProperty('--vv-alto');
      raiz.removeProperty('--vv-abajo');
      this.vistaPublicada = '';
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
