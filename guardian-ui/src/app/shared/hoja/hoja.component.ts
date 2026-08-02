import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  Output
} from '@angular/core';

/** Cada hoja sube dos niveles sobre la anterior: su velo y su superficie. */
const SALTO_POR_HOJA = 2;

/**
 * Hoja que sube desde abajo — el patrón nativo que en el móvil reemplaza al
 * diálogo centrado del escritorio.
 *
 * <p>Existe para sacar de la app las tres cosas que más delatan al navegador:
 * el <code>window.confirm</code> (que dibuja el dominio del sitio en su
 * encabezado, con la tipografía y los botones del navegador), los formularios
 * en acordeón que empujan el contenido, y los menús de utilidades apretados en
 * una esquina.</p>
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

  @Output() cerrar = new EventEmitter<void>();

  /** Nivel de apilamiento de ESTA hoja, para que dos superpuestas no empaten. */
  nivel = 0;

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

  /** Escape cierra, como cualquier diálogo del sistema. */
  @HostListener('document:keydown.escape')
  alEscape(): void {
    if (this.abierta) {
      this.cerrar.emit();
    }
  }

  /**
   * Se anuncia como aria-modal, así que el foco tiene que quedarse dentro:
   * si no, el lector de pantalla y el tabulador se van al fondo que el
   * usuario no puede ver.
   */
  @HostListener('document:keydown.tab', ['$event'])
  alTabular(evento: KeyboardEvent): void {
    if (!this.abierta) {
      return;
    }
    const focoables = this.focoables();
    if (focoables.length === 0) {
      return;
    }

    const primero = focoables[0];
    const ultimo = focoables[focoables.length - 1];
    const activo = document.activeElement;

    if (evento.shiftKey && (activo === primero || !this.host.nativeElement.contains(activo))) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && activo === ultimo) {
      evento.preventDefault();
      primero.focus();
    }
  }

  private abrir(): void {
    if (this.contabilizada) {
      return;
    }
    if (HojaComponent.abiertas === 0) {
      HojaComponent.overflowPrevio = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
    }
    this.nivel = HojaComponent.abiertas * SALTO_POR_HOJA;
    HojaComponent.abiertas++;
    this.contabilizada = true;

    this.focoPrevio = document.activeElement as HTMLElement | null;
    // Tras el render: el contenido proyectado todavía no existe en ngOnChanges.
    // El primero SIN data-no-autofoco: la X del encabezado es focoable para el
    // tabulador, pero abrir un formulario con el foco en "cerrar" no sirve.
    setTimeout(() => {
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

    if (HojaComponent.abiertas === 0) {
      document.body.style.overflow = HojaComponent.overflowPrevio;
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
