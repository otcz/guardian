/** Cómo se lee de una fila el valor de una columna filtrable. */
export type LectorColumna<T> = (fila: T) => string | number | null | undefined;

/**
 * El autofiltro de una tabla: el buscador de arriba y los desplegables por
 * columna, como en una hoja de cálculo.
 *
 * <p>Vive fuera de los componentes porque son seis tablas las que lo usan y
 * escrito seis veces se arregla en una y se olvida en las otras. Cada pantalla
 * solo declara qué columnas se filtran y cómo se lee cada una.</p>
 *
 * <p><b>Filtra en memoria, sobre las filas que la pantalla ya tiene.</b> Eso lo
 * hace instantáneo y sin una petición por tecla, pero significa que NO sirve
 * para una tabla paginada en el servidor: ahí filtraría solo la página a la
 * vista y diría "no hay nada" teniendo resultados en la siguiente.</p>
 */
export class FiltroTabla<T> {

  /** Lo que se escribe en el buscador de arriba. */
  busqueda = '';

  /** Por columna, los valores marcados. Vacío = esa columna no filtra nada. */
  private readonly marcados = new Map<string, Set<string>>();

  /**
   * @param columnas    campo → cómo leerlo de la fila. Son las que tendrán
   *                    desplegable.
   * @param textoDeFila qué texto revisa el buscador de arriba. Sin esto el
   *                    buscador mira las mismas columnas del desplegable.
   */
  constructor(
    private readonly columnas: Record<string, LectorColumna<T>>,
    private readonly textoDeFila?: (fila: T) => string
  ) {}

  /** Los valores distintos de una columna, ordenados, para pintar el menú. */
  valoresDe(campo: string, filas: T[]): string[] {
    const lector = this.columnas[campo];
    if (!lector) {
      return [];
    }
    const vistos = new Set<string>();
    for (const fila of filas) {
      vistos.add(this.textoDe(lector(fila)));
    }
    return Array.from(vistos).sort((a, b) => a.localeCompare(b, 'es'));
  }

  marcadosDe(campo: string): Set<string> {
    let seleccion = this.marcados.get(campo);
    if (!seleccion) {
      seleccion = new Set<string>();
      this.marcados.set(campo, seleccion);
    }
    return seleccion;
  }

  alternar(campo: string, valor: string): void {
    const seleccion = this.marcadosDe(campo);
    if (seleccion.has(valor)) {
      seleccion.delete(valor);
    } else {
      seleccion.add(valor);
    }
  }

  limpiar(campo: string): void {
    this.marcados.delete(campo);
  }

  limpiarTodo(): void {
    this.marcados.clear();
    this.busqueda = '';
  }

  /** Si la columna está filtrando. La cabecera lo marca para que se vea. */
  activo(campo: string): boolean {
    return (this.marcados.get(campo)?.size ?? 0) > 0;
  }

  get algunoActivo(): boolean {
    if (this.busqueda.trim()) {
      return true;
    }
    for (const seleccion of this.marcados.values()) {
      if (seleccion.size > 0) {
        return true;
      }
    }
    return false;
  }

  /** Las filas que sobreviven al buscador y a todos los desplegables. */
  aplicar(filas: T[]): T[] {
    const busqueda = this.sinTildes(this.busqueda).trim();

    return filas.filter(fila => {
      for (const [campo, seleccion] of this.marcados) {
        if (seleccion.size === 0) {
          continue;
        }
        if (!seleccion.has(this.textoDe(this.columnas[campo](fila)))) {
          return false;
        }
      }
      if (!busqueda) {
        return true;
      }
      return this.sinTildes(this.textoBuscable(fila)).includes(busqueda);
    });
  }

  // ───────────────────────────────────────────────────────────────────────────

  private textoBuscable(fila: T): string {
    if (this.textoDeFila) {
      return this.textoDeFila(fila);
    }
    return Object.values(this.columnas).map(leer => this.textoDe(leer(fila))).join(' ');
  }

  /** Null y vacío son el mismo caso y en el menú se leen igual: "(vacío)". */
  private textoDe(valor: string | number | null | undefined): string {
    if (valor === null || valor === undefined || `${valor}`.trim() === '') {
      return '(vacío)';
    }
    return `${valor}`.trim();
  }

  /**
   * Sin tildes a lado y lado: quien busca "penalo" no escribe la eñe, y no
   * encontrar a la persona lo hace concluir que no está registrada.
   */
  private sinTildes(texto: string): string {
    return texto.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '');
  }
}
