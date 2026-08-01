import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { Casa } from '../../../core/models/admin.model';

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

  readonly formulario = this.fb.nonNullable.group({
    torre: [''],
    numero: ['', [Validators.required]],
    cuposParqueadero: [0]
  });

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
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
      numero: casa.numero,
      cuposParqueadero: casa.cuposParqueadero ?? 0
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
    this.formulario.reset({ torre: '', numero: '', cuposParqueadero: 0 });
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

  activa(casa: Casa): boolean {
    return casa.activo === 'S';
  }
}
