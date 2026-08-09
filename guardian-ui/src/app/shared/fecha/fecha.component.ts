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

/** Lunes primero: es como se lee un calendario en el país. */
const DIAS = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'
];

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
export class FechaComponent implements OnDestroy {

  /** `YYYY-MM-DD`. */
  @Input() valor = '';

  /** Primer día elegible, `YYYY-MM-DD`. Los anteriores quedan apagados. */
  @Input() min: string | null = null;

  @Input() etiquetaAccesible = 'Elegir fecha';

  @Output() valorChange = new EventEmitter<string>();

  readonly dias = DIAS;

  abierto = false;

  @ViewChild('panel') private panel?: ElementRef<HTMLElement>;
  @ViewChild('campo') private campo?: ElementRef<HTMLElement>;

  /** Mes en pantalla, `YYYY-MM`. Se mueve con las flechas sin tocar el valor. */
  private mes = '';

  constructor(
    private readonly host: ElementRef<HTMLElement>,
    private readonly cd: ChangeDetectorRef
  ) {}

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
    this.abierto ? this.cerrar() : this.abrir();
  }

  private abrir(): void {
    // Se reabre siempre sobre el mes del valor: quien navegó a diciembre y
    // cerró sin elegir no quiere volver a diciembre la próxima vez.
    this.mes = (this.valor || this.hoy()).slice(0, 7);
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
   */
  private ubicar(): void {
    if (!this.abierto || !this.panel || !this.campo) {
      return;
    }
    const donde = ubicarPanel(this.campo.nativeElement, this.panel.nativeElement);
    const estilo = this.panel.nativeElement.style;
    estilo.top = `${donde.top}px`;
    estilo.left = `${donde.left}px`;
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
    this.cerrar();
  }

  /** Tocar fuera cierra. Sin esto el panel se queda abierto sobre el resto. */
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
