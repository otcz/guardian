import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { FiltroTabla } from '../../../shared/tabla/filtro-tabla';
import { Casa, ImportacionCasas, Parametro } from '../../../core/models/admin.model';

@Component({
  selector: 'gd-casas',
  templateUrl: './casas.component.html',
  styleUrl: './casas.component.scss',
  standalone: false
})
export class CasasComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  casas: Casa[] = [];

  /** Lo que la tabla pinta: las casas que pasan el autofiltro. */
  visibles: Casa[] = [];

  /**
   * Autofiltro por columna. No se filtra por identificador —es único, así que
   * cada valor dejaría una sola fila— sino por lo que agrupa: la torre, si
   * está habitada y su estado.
   */
  readonly filtro = new FiltroTabla<Casa>(
    {
      torre: c => c.torre,
      // "3" o "5" no es una pregunta que nadie se haga; "¿cuáles están vacías?"
      // sí, y es la que manda una carta de cobro o una visita.
      ocupacion: c => (c.residentes > 0 ? 'Habitada' : 'Vacía'),
      estado: c => this.estado(c)
    },
    c => `${c.identificador} ${c.torre ?? ''}`
  );
  cargando = true;
  guardando = false;
  error: string | null = null;

  /** Casa en edición. Null = el formulario está en modo alta. */
  editando: Casa | null = null;

  tiposVivienda: Parametro[] = [];

  readonly formulario = this.fb.nonNullable.group({
    torre: ['', [Validators.required]],
    numero: ['', [Validators.required]]
  });

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.parametros('TIPO_VIVIENDA').subscribe(t => {
      this.tiposVivienda = t;
      // El default se aplica al LLEGAR el catalogo, no antes: el select no
      // puede seleccionar una opcion que todavia no existe en el DOM.
      if (!this.editando && !this.formulario.controls.torre.value) {
        this.formulario.controls.torre.setValue(this.tipoPorDefecto());
      }
    });
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
    this.admin.casas().subscribe({
      next: casas => {
        this.casas = casas;
        this.filtrar();
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las casas.';
        this.cargando = false;
      }
    });
  }

  guardar(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;
    const datos = this.formulario.getRawValue();

    const peticion = this.editando
      ? this.admin.actualizarCasa(this.editando.id, datos)
      : this.admin.crearCasa(datos);

    peticion.subscribe({
      next: casa => {
        this.casas = (this.editando
          ? this.casas.map(c => (c.id === casa.id ? casa : c))
          : [...this.casas, casa])
          .sort((a, b) => a.identificador.localeCompare(b.identificador));
        this.filtrar();
        this.cancelarEdicion();
        this.guardando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar la casa.';
        this.guardando = false;
      }
    });
  }

  editar(casa: Casa): void {
    this.editando = casa;
    this.formulario.setValue({
      torre: casa.torre ?? '',
      numero: casa.numero
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
    // Vuelve al tipo por defecto, no a vacio: casi todas las altas seguidas
    // son del mismo tipo, y obligar a reelegirlo en cada una es un toque de mas.
    this.formulario.reset({ torre: this.tipoPorDefecto(), numero: '' });
  }

  alternarEstado(casa: Casa): void {
    const activar = casa.activo !== 'S';

    this.admin.cambiarEstadoCasa(casa.id, activar).subscribe({
      next: actualizada => {
        this.casas = this.casas.map(c => (c.id === actualizada.id ? actualizada : c));
        this.filtrar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  // ── Bloqueo administrativo ───────────────────────────────────────────────

  /** Casa a la que se le va a poner el candado. Null = hoja cerrada. */
  bloqueando: Casa | null = null;

  /** Casa que se va a habilitar de nuevo. Null = hoja cerrada. */
  habilitando: Casa | null = null;

  alternarBloqueo(casa: Casa): void {
    if (this.bloqueada(casa)) {
      this.habilitando = casa;
    } else {
      this.bloqueando = casa;
    }
  }

  confirmarBloqueo(motivo: string): void {
    const casa = this.bloqueando;
    if (!casa) {
      return;
    }

    this.error = null;
    this.admin.bloquear('casas', casa.id, motivo).subscribe({
      next: () => {
        this.bloqueando = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos bloquear la casa.';
        this.bloqueando = null;
      }
    });
  }

  get mensajeHabilitar(): string {
    const casa = this.habilitando;
    if (!casa) {
      return '';
    }
    return `${casa.identificador} está deshabilitada por: `
      + `${casa.motivoBloqueo || 'sin motivo registrado'}. `
      + 'Al habilitarla, sus residentes vuelven a poder ingresar.';
  }

  confirmarHabilitar(): void {
    const casa = this.habilitando;
    if (!casa) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('casas', casa.id).subscribe({
      next: () => {
        this.habilitando = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos habilitarla.';
        this.habilitando = null;
      }
    });
  }

  /**
   * CASA por defecto: es la mayoria en un conjunto de casas, y el nombre del
   * modulo lo dice. Si el administrador la ocultara desde Configuracion, cae
   * a la primera que quede — un default que apunta a una opcion inexistente
   * dejaria el select en blanco y el formulario invalido sin decir por que.
   */
  private tipoPorDefecto(): string {
    const casa = this.tiposVivienda.find(t => t.codigo === 'CASA');
    return casa?.codigo ?? this.tiposVivienda[0]?.codigo ?? '';
  }

  // ── Autofiltro ───────────────────────────────────────────────────────────

  /** Recalcula la lista visible. Se llama tras cargar y tras cada cambio. */
  filtrar(): void {
    this.visibles = this.filtro.aplicar(this.casas);
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activa(casa: Casa): boolean {
    return casa.activo === 'S';
  }

  bloqueada(casa: Casa): boolean {
    return casa.bloqueado === 'S';
  }

  /** La llave de la administración gana: deja fuera a todos sus residentes. */
  estado(casa: Casa): string {
    if (this.bloqueada(casa)) {
      return 'Deshabilitada';
    }
    return this.activa(casa) ? 'Activa' : 'Inactiva';
  }

  operativa(casa: Casa): boolean {
    return this.activa(casa) && !this.bloqueada(casa);
  }

  // ── Carga masiva ─────────────────────────────────────────────────────────

  importando = false;
  resultado: ImportacionCasas | null = null;

  /**
   * La plantilla la genera el SERVIDOR y no el navegador: sale con los tipos de
   * vivienda reales de esta sede y con las mismas columnas que lee el
   * importador, así el formato que se descarga no puede separarse del que se
   * acepta.
   */
  descargarPlantilla(): void {
    this.error = null;
    this.admin.plantillaCasas().subscribe({
      next: libro => this.descargar(libro, 'plantilla-casas.xlsx'),
      error: () => (this.error = 'No pudimos generar la plantilla.')
    });
  }

  archivoElegido(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    // Se limpia el input para que elegir el MISMO archivo dos veces seguidas
    // vuelva a disparar el evento; si no, corregir el Excel y reintentar sin
    // cambiarle el nombre no haría nada.
    entrada.value = '';
    if (!archivo) {
      return;
    }

    this.importando = true;
    this.error = null;
    this.resultado = null;

    this.admin.importarCasas(archivo).subscribe({
      next: resultado => {
        this.resultado = resultado;
        this.importando = false;
        // Se recarga siempre, aunque haya rechazos: las filas buenas ya entraron.
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cargar el archivo.';
        this.importando = false;
      }
    });
  }

  private descargar(contenido: Blob, nombre: string): void {
    const url = URL.createObjectURL(contenido);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    // Sin esto el blob se queda en memoria hasta que se cierre la pestaña.
    URL.revokeObjectURL(url);
  }
}
