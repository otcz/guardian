import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';

import { seguirAlCampo, ubicarPanel } from '../panel-flotante';

/** El final del día. No es una hora a la que se cite a nadie: es "hasta que se acabe". */
const FIN_DEL_DIA = '23:59';

/**
 * Cuánto salta la flecha de los minutos.
 *
 * <p>Un cuarto de hora: a una visita se le dice "a las siete y cuarto", nunca
 * "a las siete y diecinueve". Con paso de un minuto habría que pulsar la
 * flecha cuarenta y cinco veces para cruzar una hora.</p>
 */
const PASO_MINUTOS = 15;

const MINUTOS_DEL_DIA = 24 * 60;

@Component({
  selector: 'gd-hora',
  templateUrl: './hora.component.html',
  styleUrl: './hora.component.scss',
  standalone: false
})
export class HoraComponent implements OnDestroy {

  /** `HH:mm` en 24 h. Se guarda así para poder ordenar y comparar como texto. */
  @Input() valor = '08:00';

  /** Ofrece "final del día" (23:59). Solo tiene sentido en el fin de un rango. */
  @Input() conFinDelDia = false;

  @Input() etiquetaAccesible = 'Elegir hora';

  @Output() valorChange = new EventEmitter<string>();

  abierto = false;

  @ViewChild('panel') private panel?: ElementRef<HTMLElement>;
  @ViewChild('campo') private campo?: ElementRef<HTMLElement>;

  constructor(
    private readonly host: ElementRef<HTMLElement>,
    private readonly cd: ChangeDetectorRef
  ) {}

  /** `8:30 p.m.`, o `Final del día` cuando son las 23:59. */
  get texto(): string {
    return this.esFinDelDia
      ? 'Final del día'
      : `${this.horaTexto}:${this.minutoTexto} ${this.mitad}`;
  }

  get esFinDelDia(): boolean {
    return this.valor === FIN_DEL_DIA;
  }

  /** 1 a 12: es lo que dice un reloj. Las 0 y las 12 se leen las dos "12". */
  get horaTexto(): string {
    const h = this.total / 60 | 0;
    return String(h % 12 === 0 ? 12 : h % 12);
  }

  get minutoTexto(): string {
    return this.dos(this.total % 60);
  }

  get mitad(): string {
    return this.total < MINUTOS_DEL_DIA / 2 ? 'a.m.' : 'p.m.';
  }

  alternar(): void {
    this.abierto ? this.cerrar() : this.abrir();
  }

  private abrir(): void {
    this.abierto = true;
    // Pintar el panel AHORA y medirlo en el mismo turno. Con un setTimeout la
    // medida corría antes de que la vista existiera y el panel se quedaba en
    // la esquina; y como todo pasa sin ceder el hilo, no hay un fotograma
    // intermedio donde se vea saltar.
    this.cd.detectChanges();
    this.ubicar();
    this.soltar = seguirAlCampo(() => this.ubicar(), () => this.cerrarYRefrescar());
  }

  private cerrar(): void {
    this.abierto = false;
    this.soltar?.();
    this.soltar = undefined;
  }

  /** Cerrar desde un evento de fuera de Angular necesita avisar a la vista. */
  private cerrarYRefrescar(): void {
    this.cerrar();
    this.cd.detectChanges();
  }

  ngOnDestroy(): void {
    this.cerrar();
  }

  /** Suelta los escuchas del panel abierto. */
  private soltar?: () => void;

  /**
   * Se escribe en el estilo del elemento y no por binding: el binding depende
   * de otro ciclo de detección, y hasta que llegue el panel ya se dibujó.
   *
   * <p>Pegado al borde DERECHO del campo: es el control de más a la derecha de
   * su fila, y así el panel crece hacia adentro del formulario.</p>
   */
  private ubicar(): void {
    if (!this.abierto || !this.panel || !this.campo) {
      return;
    }
    const donde = ubicarPanel(this.campo.nativeElement, this.panel.nativeElement, 'derecha');
    const estilo = this.panel.nativeElement.style;
    estilo.top = `${donde.top}px`;
    estilo.left = `${donde.left}px`;
  }

  correrHora(pasos: number): void {
    this.fijar(this.total + pasos * 60);
  }

  /**
   * Al siguiente cuarto de hora, no "sumar quince".
   *
   * <p>Desde las 8:07 la flecha lleva a las 8:15 y no a las 8:22. Sumar el
   * paso conserva el desfase para siempre, y basta con que el valor venga de
   * otra parte —del final del día, que son las 23:59— para que el selector
   * quede pegado a minutos que nadie eligió.</p>
   */
  correrMinutos(pasos: number): void {
    const bloques = pasos > 0
      ? Math.floor(this.total / PASO_MINUTOS) + 1
      : Math.ceil(this.total / PASO_MINUTOS) - 1;
    this.fijar(bloques * PASO_MINUTOS);
  }

  /** Doce horas de diferencia: es exactamente lo que separa a.m. de p.m. */
  alternarMitad(): void {
    this.fijar(this.total + MINUTOS_DEL_DIA / 2);
  }

  elegirFinDelDia(): void {
    this.valor = FIN_DEL_DIA;
    this.valorChange.emit(FIN_DEL_DIA);
  }

  @HostListener('document:pointerdown', ['$event'])
  alTocarFuera(evento: Event): void {
    if (this.abierto && !this.host.nativeElement.contains(evento.target as Node)) {
      this.cerrar();
    }
  }

  @HostListener('document:keydown.escape')
  alEscapar(): void {
    this.cerrar();
  }

  /** Minutos transcurridos del día. Toda la aritmética del selector vive acá. */
  private get total(): number {
    const [h, m] = this.valor.split(':').map(Number);
    return h * 60 + m;
  }

  /** Da la vuelta al día en los dos sentidos: de 11 p.m. se sigue a 12 a.m. */
  private fijar(minutos: number): void {
    const normalizado = ((minutos % MINUTOS_DEL_DIA) + MINUTOS_DEL_DIA) % MINUTOS_DEL_DIA;
    const nuevo = `${this.dos(normalizado / 60 | 0)}:${this.dos(normalizado % 60)}`;
    this.valor = nuevo;
    this.valorChange.emit(nuevo);
  }

  private dos(n: number): string {
    return String(n).padStart(2, '0');
  }
}
