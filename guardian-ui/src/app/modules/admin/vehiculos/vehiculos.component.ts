import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { Casa, Parametro, Vehiculo } from '../../../core/models/admin.model';

@Component({
  selector: 'gd-admin-vehiculos',
  templateUrl: './vehiculos.component.html',
  styleUrl: './vehiculos.component.scss',
  standalone: false
})
export class VehiculosComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  vehiculos: Vehiculo[] = [];
  casas: Casa[] = [];
  tipos: Parametro[] = [];

  cargando = true;
  guardando = false;
  error: string | null = null;
  mostrarAlta = false;

  readonly formulario = this.fb.nonNullable.group({
    casaId: [null as number | null, [Validators.required]],
    placa: ['', [Validators.required]],
    tipo: ['', [Validators.required]],
    marca: [''],
    color: ['']
  });

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.casas().subscribe(casas => (this.casas = casas));
    this.admin.parametros('TIPO_VEHICULO').subscribe(tipos => (this.tipos = tipos));
  }

  cargar(): void {
    this.cargando = true;
    this.admin.vehiculos().subscribe({
      next: vehiculos => {
        this.vehiculos = vehiculos;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar los vehículos.';
        this.cargando = false;
      }
    });
  }

  crear(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    const datos = this.formulario.getRawValue();
    this.guardando = true;
    this.error = null;

    this.admin
      .crearVehiculo({ ...datos, casaId: datos.casaId! })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarAlta = false;
          this.formulario.reset();
          this.cargar();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar el vehículo.';
        }
      });
  }

  alternarEstado(vehiculo: Vehiculo): void {
    this.error = null;
    this.admin.cambiarEstadoVehiculo(vehiculo.id, vehiculo.activo !== 'S').subscribe({
      next: actualizado => {
        this.vehiculos = this.vehiculos.map(v => (v.id === actualizado.id ? actualizado : v));
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  eliminar(vehiculo: Vehiculo): void {
    const seguro = window.confirm(
      `¿Eliminar definitivamente el vehículo ${vehiculo.placa}? ` +
      'La bitácora conservará sus registros de acceso.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.eliminarVehiculo(vehiculo.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos eliminar el vehículo.';
      }
    });
  }

  activo(vehiculo: Vehiculo): boolean {
    return vehiculo.activo === 'S';
  }
}
