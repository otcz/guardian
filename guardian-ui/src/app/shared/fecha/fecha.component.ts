import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output
} from '@angular/core';

/** Lunes primero: es como se lee un calendario en el país. */
const DIAS = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

/** Alto aproximado del panel, para decidir hacia dónde abrirlo. */
const ALTO_PANEL = 320;

/**
 * Un calendario propio para elegir un día.
 *
 * <p>Reemplaza al `<input type="date">`. El campo nativo abre el calendario
 * DEL NAVEGADOR: no hereda ningún color de la aplicación, así que en el modo
 * claro se abría un panel oscuro encima del formulario, y en un teléfono se
 * abre a pantalla completa tapando todo lo demás. Un control que no se puede
 * tematizar no sirve en una aplicación que tiene modo claro y oscuro.</p>
 *
 * <p>Trabaja con la fecha como texto `YYYY-MM-DD` y nunca con `Date` en el
 * valor: convertir a `Date` y volver arrastra la zona horaria, y una visita
 * agendada para el 9 terminaba guardada el 8.</p>
 */
@Component({
  selector: 'gd-fecha',
  templateUrl: './fecha.component.html',
  styleUrl: './fecha.component.scss',
  standalone: false
})
export class FechaComponent {

  /** `YYYY-MM-DD`. */
  @Input() valor = '';

  /** Primer día elegible, `YYYY-MM-DD`. Los anteriores quedan apagados. */
  @Input() min: string | null = null;

  @Input() etiquetaAccesible = 'Elegir fecha';

  @Output() valorChange = new EventEmitter<string>();

  readonly dias = DIAS;

  abierto = false;

  /** El panel se despliega hacia arriba cuando abajo no cabe. */
  arriba = false;

  /** Mes en pantalla, `YYYY-MM`. Se mueve con las flechas sin tocar el valor. */
  private mes = '';

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  /** Lo que se lee en el campo cerrado: `sáb 8 ago`. */
  get texto(): string {
    if (!this.valor) {
      return 'Elegir día';
    }
    const [a, m, d] = this.partes(this.valor);
    const fecha = new Date(a, m - 1, d);
    // getDay() cuenta desde el domingo; la fila arranca en lunes.
    const dia = DIAS[(fecha.getDay() + 6) % 7];
    return `${dia} ${d} ${MESES[m - 1].slice(0, 3)}`;
  }

  get tituloMes(): string {
    const [a, m] = this.partes(`${this.mesVisible}-01`);
    return `${MESES[m - 1]} ${a}`;
  }

  /** Las seis filas del mes; `null` en los huecos de antes y después. */
  get semanas(): (string | null)[][] {
    const [a, m] = this.partes(`${this.mesVisible}-01`);
    const primero = new Date(a, m - 1, 1);
    const hueco = (primero.getDay() + 6) % 7;
    const cuantos = new Date(a, m, 0).getDate();

    const celdas: (string | null)[] = Array(hueco).fill(null);
    for (let d = 1; d <= cuantos; d++) {
      celdas.push(`${a}-${this.dos(m)}-${this.dos(d)}`);
    }
    while (celdas.length % 7 !== 0) {
      celdas.push(null);
    }

    const filas: (string | null)[][] = [];
    for (let i = 0; i < celdas.length; i += 7) {
      filas.push(celdas.slice(i, i + 7));
    }
    return filas;
  }

  /** El mes que se está viendo. Arranca en el del valor, no en el de hoy. */
  get mesVisible(): string {
    return this.mes || (this.valor || this.hoy()).slice(0, 7);
  }

  numeroDe(dia: string): number {
    return this.partes(dia)[2];
  }

  esElegido(dia: string): boolean {
    return dia === this.valor;
  }

  esHoy(dia: string): boolean {
    return dia === this.hoy();
  }

  /** Antes del mínimo no se puede citar a nadie. */
  esApagado(dia: string): boolean {
    return !!this.min && dia < this.min;
  }

  alternar(): void {
    this.abierto = !this.abierto;
    if (this.abierto) {
      // Se reabre siempre sobre el mes del valor: quien navegó a diciembre y
      // cerró sin elegir no quiere volver a diciembre la próxima vez.
      this.mes = (this.valor || this.hoy()).slice(0, 7);
      this.decidirLado(ALTO_PANEL);
    }
  }

  /**
   * Abre hacia el lado donde quepa.
   *
   * <p>La hoja recorta lo que se sale de su cuerpo, así que un panel que se
   * despliega hacia abajo cuando el campo está en la parte baja de la pantalla
   * queda fuera de vista y solo aparece si el usuario adivina que tiene que
   * desplazar. Hacia arriba solo se abre si arriba SÍ cabe: si no cabe en
   * ninguno de los dos lados, abajo es lo esperable.</p>
   */
  private decidirLado(alto: number): void {
    const caja = this.host.nativeElement.getBoundingClientRect();
    this.arriba = window.innerHeight - caja.bottom < alto && caja.top > alto;
  }

  correrMes(pasos: number): void {
    const [a, m] = this.partes(`${this.mesVisible}-01`);
    const destino = new Date(a, m - 1 + pasos, 1);
    this.mes = `${destino.getFullYear()}-${this.dos(destino.getMonth() + 1)}`;
  }

  elegir(dia: string | null): void {
    if (!dia || this.esApagado(dia)) {
      return;
    }
    this.valor = dia;
    this.valorChange.emit(dia);
    this.abierto = false;
  }

  /** Tocar fuera cierra. Sin esto el panel se queda abierto sobre el resto. */
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

  private hoy(): string {
    const ahora = new Date();
    return `${ahora.getFullYear()}-${this.dos(ahora.getMonth() + 1)}-${this.dos(ahora.getDate())}`;
  }

  private partes(iso: string): [number, number, number] {
    const [a, m, d] = iso.split('-').map(Number);
    return [a, m, d];
  }

  private dos(n: number): string {
    return String(n).padStart(2, '0');
  }
}
