import { Component, OnInit } from '@angular/core';

import { AccesoService } from '../../../core/services/acceso.service';
import { AdminService } from '../../../core/services/admin.service';
import { AccesoEvento, Resultado } from '../../../core/models/acceso.model';
import { Parametro, Porteria } from '../../../core/models/admin.model';
import { FiltroTabla } from '../../../shared/tabla/filtro-tabla';

/** Una opción del desplegable: lo que se lee arriba, lo que viaja abajo. */
interface Opcion {
  codigo: string;
  etiqueta: string;
}

/**
 * Bitácora de accesos: la auditoría del conjunto. Todo intento queda —
 * permitido o denegado — y de acá salen las respuestas a "¿quién entró
 * anoche?" y "¿quién insiste con un QR revocado?".
 *
 * <p><b>Acá el autofiltro trabaja en el SERVIDOR.</b> Es la única tabla del
 * panel que pagina, y filtrar en memoria sobre la página a la vista diría "no
 * hay movimientos" con el evento esperando en la página siguiente — que en una
 * auditoría es la peor respuesta posible.</p>
 */
@Component({
  selector: 'gd-admin-bitacora',
  templateUrl: './bitacora.component.html',
  styleUrl: './bitacora.component.scss',
  standalone: false
})
export class BitacoraComponent implements OnInit {

  eventos: AccesoEvento[] = [];
  totalEventos = 0;
  pagina = 0;
  readonly tamano = 25;

  cargando = true;
  error: string | null = null;

  /**
   * Solo guarda lo marcado: el filtro de verdad lo aplica el servidor. Por eso
   * se construye sin columnas — no hay nada que filtrar en memoria.
   */
  readonly filtro = new FiltroTabla<AccesoEvento>({});

  /**
   * Catálogo de motivos, para traducir el código a texto legible. Se pide una
   * vez y se le pasa al pipe: sin él la tabla mostraría PERSONA_BLOQUEADA en
   * crudo, que es un dato de base de datos, no una explicación.
   */
  motivos: Parametro[] = [];

  /** Las porterías del conjunto, para el desplegable de esa columna. */
  porterias: Porteria[] = [];

  // Fijos y no deducidos de las filas: en una tabla paginada, lo que hay en la
  // página a la vista no es lo que hay en la columna.
  private readonly SENTIDOS: Opcion[] = [
    { codigo: 'E', etiqueta: 'Entrada' },
    { codigo: 'S', etiqueta: 'Salida' }
  ];

  private readonly MODOS: Opcion[] = [
    { codigo: 'PEATON', etiqueta: 'A pie' },
    { codigo: 'VEHICULO', etiqueta: 'Vehículo' }
  ];

  private readonly RESULTADOS: Opcion[] = [
    { codigo: 'PERMITIDO', etiqueta: 'Permitido' },
    { codigo: 'DENEGADO', etiqueta: 'Denegado' }
  ];

  constructor(
    private readonly acceso: AccesoService,
    private readonly admin: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.parametros('MOTIVO_DENEGACION').subscribe(m => (this.motivos = m));
    this.admin.porterias().subscribe(p => (this.porterias = p));
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;

    this.acceso
      .eventos({
        resultados: this.codigosDe('resultado', this.RESULTADOS) as Resultado[],
        sentidos: this.codigosDe('sentido', this.SENTIDOS),
        modos: this.codigosDe('modo', this.MODOS),
        motivos: this.codigosDe('motivo', this.opcionesDeMotivo),
        porteriaIds: this.porteriasMarcadas,
        texto: this.filtro.busqueda,
        pagina: this.pagina,
        tamano: this.tamano
      })
      .subscribe({
        next: respuesta => {
          this.eventos = respuesta.content;
          this.totalEventos = respuesta.totalElements;
          this.cargando = false;
        },
        error: () => {
          this.error = 'No pudimos cargar la bitácora.';
          this.cargando = false;
        }
      });
  }

  /**
   * Cambiar un filtro vuelve a la primera página. Quedarse en la 7 con un
   * filtro nuevo mostraría una tabla vacía y parecería que no hay resultados.
   */
  filtrar(): void {
    this.pagina = 0;
    this.cargar();
  }

  // ── Opciones de cada desplegable ─────────────────────────────────────────

  get opcionesSentido(): string[] {
    return this.SENTIDOS.map(o => o.etiqueta);
  }

  get opcionesModo(): string[] {
    return this.MODOS.map(o => o.etiqueta);
  }

  get opcionesResultado(): string[] {
    return this.RESULTADOS.map(o => o.etiqueta);
  }

  get opcionesMotivo(): string[] {
    return this.opcionesDeMotivo.map(o => o.etiqueta);
  }

  get opcionesPorteria(): string[] {
    return this.porterias.map(p => p.nombre);
  }

  // ─────────────────────────────────────────────────────────────────────────

  private get opcionesDeMotivo(): Opcion[] {
    return this.motivos.map(m => ({ codigo: m.codigo, etiqueta: m.valor }));
  }

  /**
   * Traduce lo marcado —que son etiquetas, lo que el administrador lee— a los
   * códigos que entiende el servidor.
   */
  private codigosDe(campo: string, opciones: Opcion[]): string[] {
    const marcadas = this.filtro.marcadosDe(campo);
    return opciones.filter(o => marcadas.has(o.etiqueta)).map(o => o.codigo);
  }

  private get porteriasMarcadas(): number[] {
    const marcadas = this.filtro.marcadosDe('porteria');
    return this.porterias.filter(p => marcadas.has(p.nombre)).map(p => p.id);
  }

  irA(pagina: number): void {
    this.pagina = pagina;
    this.cargar();
  }

  get totalPaginas(): number {
    return Math.ceil(this.totalEventos / this.tamano);
  }

  permitido(evento: AccesoEvento): boolean {
    return evento.resultado === 'PERMITIDO';
  }
}
