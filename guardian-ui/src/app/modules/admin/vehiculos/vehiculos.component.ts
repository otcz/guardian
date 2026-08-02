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

  /** Vehículo en edición. Null = el formulario está en modo alta. */
  editando: Vehiculo | null = null;

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

  guardar(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    const datos = this.formulario.getRawValue();
    const request = { ...datos, casaId: datos.casaId! };
    this.guardando = true;
    this.error = null;

    const peticion = this.editando
      ? this.admin.actualizarVehiculo(this.editando.id, request)
      : this.admin.crearVehiculo(request);

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cancelarEdicion();
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar el vehículo.';
      }
    });
  }

  editar(vehiculo: Vehiculo): void {
    this.editando = vehiculo;
    this.mostrarAlta = true;
    this.formulario.setValue({
      casaId: vehiculo.casaId,
      placa: vehiculo.placa,
      tipo: vehiculo.tipo,
      marca: vehiculo.marca ?? '',
      color: vehiculo.color ?? ''
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
    this.mostrarAlta = false;
    this.formulario.reset();
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

  // ── Bloqueo administrativo ───────────────────────────────────────────────

  /** Vehículo al que se le va a poner el candado. Null = hoja cerrada. */
  bloqueando: Vehiculo | null = null;

  alternarBloqueo(vehiculo: Vehiculo): void {
    if (this.bloqueado(vehiculo)) {
      this.desbloquear(vehiculo);
    } else {
      this.bloqueando = vehiculo;
    }
  }

  confirmarBloqueo(motivo: string): void {
    const vehiculo = this.bloqueando;
    if (!vehiculo) {
      return;
    }

    this.error = null;
    this.admin.bloquear('vehiculos', vehiculo.id, motivo).subscribe({
      next: () => {
        this.bloqueando = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos bloquear el vehículo.';
        this.bloqueando = null;
      }
    });
  }

  private desbloquear(vehiculo: Vehiculo): void {
    const seguro = window.confirm(
      `${vehiculo.placa} está bloqueado por: ` +
      `${vehiculo.motivoBloqueo || 'sin motivo registrado'}.\n\n` +
      '¿Levantar el bloqueo? Volverá a aparecer en la portería.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('vehiculos', vehiculo.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos levantar el bloqueo.';
      }
    });
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activo(vehiculo: Vehiculo): boolean {
    return vehiculo.activo === 'S';
  }

  bloqueado(vehiculo: Vehiculo): boolean {
    return vehiculo.bloqueado === 'S';
  }

  /** El bloqueo gana: un carro bloqueado no sale aunque esté habilitado. */
  estado(vehiculo: Vehiculo): string {
    if (this.bloqueado(vehiculo)) {
      return 'Bloqueado';
    }
    return this.activo(vehiculo) ? 'Habilitado' : 'Inhabilitado';
  }

  operativo(vehiculo: Vehiculo): boolean {
    return this.activo(vehiculo) && !this.bloqueado(vehiculo);
  }
}
