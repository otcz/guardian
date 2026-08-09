import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output
} from '@angular/core';

/** El final del día. No es una hora a la que se cite a nadie: es "hasta que se acabe". */
const FIN_DEL_DIA = '23:59';

/** Alto aproximado del panel, para decidir hacia dónde abrirlo. */
const ALTO_PANEL = 190;

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
export class HoraComponent {

  /** `HH:mm` en 24 h. Se guarda así para poder ordenar y comparar como texto. */
  @Input() valor = '08:00';

  /** Ofrece "final del día" (23:59). Solo tiene sentido en el fin de un rango. */
  @Input() conFinDelDia = false;

  @Input() etiquetaAccesible = 'Elegir hora';

  @Output() valorChange = new EventEmitter<string>();

  abierto = false;

  /** El panel se despliega hacia arriba cuando abajo no cabe. */
  arriba = false;

  constructor(private readonly host: ElementRef<HTMLElement>) {}

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
    this.abierto = !this.abierto;
    if (this.abierto) {
      // La hoja recorta lo que se sale de su cuerpo: abajo del todo, un panel
      // que baja queda fuera de vista hasta que alguien adivine que hay que
      // desplazar. Hacia arriba solo si arriba SÍ cabe.
      const caja = this.host.nativeElement.getBoundingClientRect();
      this.arriba = window.innerHeight - caja.bottom < ALTO_PANEL && caja.top > ALTO_PANEL;
    }
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
      this.abierto = false;
    }
  }

  @HostListener('document:keydown.escape')
  alEscapar(): void {
    this.abierto = false;
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
