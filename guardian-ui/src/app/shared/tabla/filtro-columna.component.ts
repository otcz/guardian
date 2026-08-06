import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output
} from '@angular/core';

import { FiltroTabla } from './filtro-tabla';

/**
 * El desplegable de una columna, como el autofiltro de una hoja de cálculo:
 * los valores que existen en esa columna, cada uno con su casilla.
 *
 * <p>Los valores salen de las filas que hay, no de un catálogo: si nadie tiene
 * moto, "Moto" no aparece. Un menú con opciones que no filtran nada obliga a
 * probarlas para descubrir que están vacías.</p>
 */
@Component({
  selector: 'gd-filtro-columna',
  templateUrl: './filtro-columna.component.html',
  styleUrl: './filtro-columna.component.scss',
  standalone: false
})
export class FiltroColumnaComponent {

  /** Nombre de la columna dentro del FiltroTabla de la pantalla. */
  @Input({ required: true }) campo!: string;

  /**
   * `any` a propósito: este componente no sabe ni le importa qué hay en cada
   * fila —solo pide los valores de una columna—, y tiparlo con el tipo real
   * haría que un FiltroTabla<Vehiculo> no fuera asignable a uno de Persona.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  @Input({ required: true }) filtro!: FiltroTabla<any>;

  /** Las filas SIN filtrar: el menú tiene que seguir ofreciendo lo que se ocultó. */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  @Input() filas: any[] = [];

  /**
   * Opciones fijas, cuando NO se pueden deducir de las filas.
   *
   * <p>Es el caso de una tabla que pagina en el servidor: los valores que hay
   * en la página a la vista no son los que hay en la columna, así que el menú
   * se arma con el catálogo real —los dos sentidos, los dos resultados, las
   * porterías del conjunto— y no con una muestra.</p>
   */
  @Input() opciones: string[] | null = null;

  /** La pantalla recalcula su lista visible. */
  @Output() cambio = new EventEmitter<void>();

  abierto = false;
  busqueda = '';

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  get valores(): string[] {
    const todos = this.opciones ?? this.filtro.valoresDe(this.campo, this.filas);
    const busqueda = this.busqueda.trim().toLowerCase();
    if (!busqueda) {
      return todos;
    }
    return todos.filter(v => v.toLowerCase().includes(busqueda));
  }

  get activo(): boolean {
    return this.filtro.activo(this.campo);
  }

  /** Cuántos hay marcados. Va en la cabecera para no tener que abrir el menú. */
  get cuantos(): number {
    return this.filtro.marcadosDe(this.campo).size;
  }

  marcado(valor: string): boolean {
    return this.filtro.marcadosDe(this.campo).has(valor);
  }

  alternarMenu(): void {
    this.abierto = !this.abierto;
    this.busqueda = '';
  }

  alternar(valor: string): void {
    this.filtro.alternar(this.campo, valor);
    this.cambio.emit();
  }

  limpiar(): void {
    this.filtro.limpiar(this.campo);
    this.cambio.emit();
  }

  /** Marca lo que se está viendo: con el buscador puesto, marca solo el resultado. */
  marcarTodos(): void {
    for (const valor of this.valores) {
      if (!this.marcado(valor)) {
        this.filtro.alternar(this.campo, valor);
      }
    }
    this.cambio.emit();
  }

  // Un menú abierto que no se cierra al tocar afuera se queda tapando la tabla
  // que uno acaba de filtrar.
  @HostListener('document:click', ['$event.target'])
  alTocarAfuera(objetivo: HTMLElement): void {
    if (this.abierto && !this.host.nativeElement.contains(objetivo)) {
      this.abierto = false;
    }
  }

  @HostListener('document:keydown.escape')
  alPresionarEscape(): void {
    this.abierto = false;
  }
}
