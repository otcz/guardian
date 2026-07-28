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

  crear(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    this.admin.crearCasa(this.formulario.getRawValue()).subscribe({
      next: casa => {
        this.casas = [...this.casas, casa].sort((a, b) =>
          a.identificador.localeCompare(b.identificador));
        this.formulario.reset({ torre: '', numero: '', cuposParqueadero: 0 });
        this.guardando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos crear la casa.';
        this.guardando = false;
      }
    });
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
