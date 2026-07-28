import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResidenteService } from '../../../core/services/residente.service';
import { AdminService } from '../../../core/services/admin.service';
import {
  Familiar,
  Parametro,
  Vehiculo
} from '../../../core/models/admin.model';

/**
 * Autogestión del residente: su núcleo familiar (esposa, hijos, invitados) y
 * los vehículos de su casa. Aquí no existe eliminar — solo activar/inactivar;
 * la eliminación es exclusiva del administrador.
 */
@Component({
  selector: 'gd-mi-hogar',
  templateUrl: './mi-hogar.component.html',
  styleUrl: './mi-hogar.component.scss',
  standalone: false
})
export class MiHogarComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  familia: Familiar[] = [];
  vehiculos: Vehiculo[] = [];
  parentescos: Parametro[] = [];
  tiposVehiculo: Parametro[] = [];

  cargando = true;
  sinCasa = false;
  error: string | null = null;

  mostrarAltaFamiliar = false;
  mostrarAltaVehiculo = false;
  guardando = false;

  readonly formularioFamiliar = this.fb.nonNullable.group({
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    fechaNacimiento: [''],
    fotoUrl: [null as string | null],
    telefono: [''],
    parentesco: ['', [Validators.required]]
  });

  readonly formularioVehiculo = this.fb.nonNullable.group({
    placa: ['', [Validators.required]],
    tipo: ['', [Validators.required]],
    marca: [''],
    color: ['']
  });

  constructor(
    private readonly residente: ResidenteService,
    private readonly admin: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
    // El catálogo de parentescos excluye TITULAR en el formulario: ese lo
    // asigna la administración.
    this.admin.parametros('PARENTESCO').subscribe(parametros => {
      this.parentescos = parametros.filter(p => p.codigo !== 'TITULAR');
    });
    this.admin.parametros('TIPO_VEHICULO').subscribe(tipos => (this.tiposVehiculo = tipos));
  }

  cargar(): void {
    this.cargando = true;

    this.residente.familia().subscribe({
      next: familia => {
        this.familia = familia;
        this.cargando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        if (fallo.status === 400) {
          this.sinCasa = true;
        } else {
          this.error = fallo.error?.mensaje ?? 'No pudimos cargar tu hogar.';
        }
      }
    });

    this.residente.vehiculos().subscribe({
      next: vehiculos => (this.vehiculos = vehiculos),
      error: () => undefined
    });
  }

  // ── Familia ──────────────────────────────────────────────────────────────

  agregarFamiliar(): void {
    if (this.formularioFamiliar.invalid || this.guardando) {
      this.formularioFamiliar.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;
    const datos = this.formularioFamiliar.getRawValue();

    this.residente
      .agregarFamiliar({
        ...datos,
        fechaNacimiento: datos.fechaNacimiento || null,
        telefono: datos.telefono || null
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarAltaFamiliar = false;
          this.formularioFamiliar.reset({ fotoUrl: null });
          this.cargar();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar a la persona.';
        }
      });
  }

  alternarEstadoFamiliar(familiar: Familiar): void {
    this.error = null;
    this.residente
      .cambiarEstadoFamiliar(familiar.personaId, familiar.activo !== 'S')
      .subscribe({
        next: () => this.cargar(),
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
        }
      });
  }

  // ── Vehículos ────────────────────────────────────────────────────────────

  agregarVehiculo(): void {
    if (this.formularioVehiculo.invalid || this.guardando) {
      this.formularioVehiculo.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    this.residente.agregarVehiculo(this.formularioVehiculo.getRawValue()).subscribe({
      next: () => {
        this.guardando = false;
        this.mostrarAltaVehiculo = false;
        this.formularioVehiculo.reset();
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos registrar el vehículo.';
      }
    });
  }

  alternarEstadoVehiculo(vehiculo: Vehiculo): void {
    this.error = null;
    this.residente.cambiarEstadoVehiculo(vehiculo.id, vehiculo.activo !== 'S').subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  etiquetaParentesco(codigo: string): string {
    return this.parentescos.find(p => p.codigo === codigo)?.valor ?? codigo;
  }

  activo(estado: string): boolean {
    return estado === 'S';
  }
}
