import { Component, OnInit } from '@angular/core';

import { AccesoService } from '../../../core/services/acceso.service';
import { AdminService } from '../../../core/services/admin.service';
import { AccesoEvento, Resultado } from '../../../core/models/acceso.model';
import { Parametro } from '../../../core/models/admin.model';

/**
 * Bitácora de accesos: la auditoría del conjunto. Todo intento queda —
 * permitido o denegado — y de acá salen las respuestas a "¿quién entró
 * anoche?" y "¿quién insiste con un QR revocado?".
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

  filtroResultado: Resultado | '' = '';
  cargando = true;
  error: string | null = null;

  /**
   * Catálogo de motivos, para traducir el código a texto legible. Se pide una
   * vez y se le pasa al pipe: sin él la tabla mostraría PERSONA_BLOQUEADA en
   * crudo, que es un dato de base de datos, no una explicación.
   */
  motivos: Parametro[] = [];

  constructor(
    private readonly acceso: AccesoService,
    private readonly admin: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.parametros('MOTIVO_DENEGACION').subscribe(m => (this.motivos = m));
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
    this.acceso
      .eventos({
        resultado: this.filtroResultado || null,
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

  filtrar(resultado: Resultado | ''): void {
    this.filtroResultado = resultado;
    this.pagina = 0;
    this.cargar();
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
