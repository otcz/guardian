import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { Casa, Parametro } from '../../../core/models/admin.model';

@Component({
  selector: 'gd-casas',
  templateUrl: './casas.component.html',
  styleUrl: './casas.component.scss',
  standalone: false
})
export class CasasComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  casas: Casa[] = [];
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
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  // ── Bloqueo administrativo ───────────────────────────────────────────────

  /** Casa a la que se le va a poner el candado. Null = hoja cerrada. */
  bloqueando: Casa | null = null;

  alternarBloqueo(casa: Casa): void {
    if (this.bloqueada(casa)) {
      this.desbloquear(casa);
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

  private desbloquear(casa: Casa): void {
    const seguro = window.confirm(
      `${casa.identificador} está deshabilitada por: ` +
      `${casa.motivoBloqueo || 'sin motivo registrado'}.\n\n` +
      '¿Habilitarla de nuevo? Sus residentes vuelven a ingresar.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('casas', casa.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos habilitarla.';
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
}
