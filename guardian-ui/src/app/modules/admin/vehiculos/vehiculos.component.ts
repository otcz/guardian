import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { Casa, Parametro, Vehiculo } from '../../../core/models/admin.model';
import { FiltroTabla } from '../../../shared/tabla/filtro-tabla';

@Component({
  selector: 'gd-admin-vehiculos',
  templateUrl: './vehiculos.component.html',
  styleUrl: './vehiculos.component.scss',
  standalone: false
})
export class VehiculosComponent implements OnInit {

  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  vehiculos: Vehiculo[] = [];

  /** Lo que la tabla pinta: los vehículos que pasan el autofiltro. */
  visibles: Vehiculo[] = [];

  /**
   * Autofiltro por columna, como en una hoja de cálculo. Los valores del menú
   * salen de las filas que hay, no de los catálogos: ofrecer "Moto" cuando
   * nadie tiene moto obliga a probarlo para descubrir que está vacío.
   */
  readonly filtro = new FiltroTabla<Vehiculo>(
    {
      casa: v => v.casaIdentificador,
      tipo: v => v.tipoNombre ?? v.tipo,
      marca: v => v.marcaNombre,
      color: v => v.colorNombre,
      estado: v => this.estado(v)
    },
    v => `${v.placa} ${v.casaIdentificador} ${v.marcaNombre ?? ''} ${v.colorNombre ?? ''}`
  );

  casas: Casa[] = [];
  tipos: Parametro[] = [];
  marcas: Parametro[] = [];
  colores: Parametro[] = [];

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
    color: [''],
    // Sin Validators: un vehículo sin foto es un estado legítimo. Lo identifica
    // la placa, que es única en todo el sistema.
    fotoUrl: [null as string | null]
  });

  /**
   * Borrar es del operador de la plataforma y de nadie más. El administrador
   * de la sede desactiva, bloquea y revoca — todo eso deja rastro. Borrar no:
   * la fila desaparece y con ella el registro que podría resolver una disputa.
   * El backend lo exige igual; esto solo evita ofrecer un botón que va a
   * responder 403.
   */
  readonly puedeEliminar = this.auth.tieneRol('SUPER_ADMIN');

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.casas().subscribe(casas => (this.casas = casas));
    this.admin.parametros('TIPO_VEHICULO').subscribe(tipos => (this.tipos = tipos));
    this.admin.parametros('MARCA_VEHICULO').subscribe(marcas => (this.marcas = marcas));
    this.admin.parametros('COLOR_VEHICULO').subscribe(colores => (this.colores = colores));
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
    this.admin.vehiculos().subscribe({
      next: vehiculos => {
        this.vehiculos = vehiculos;
        this.filtrar();
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
      color: vehiculo.color ?? '',
      fotoUrl: vehiculo.fotoUrl
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
        this.filtrar();
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

  /** Vehículo que se va a habilitar de nuevo. Null = hoja cerrada. */
  habilitando: Vehiculo | null = null;

  alternarBloqueo(vehiculo: Vehiculo): void {
    if (this.bloqueado(vehiculo)) {
      this.habilitando = vehiculo;
    } else {
      this.bloqueando = vehiculo;
    }
  }

  get mensajeHabilitar(): string {
    const vehiculo = this.habilitando;
    if (!vehiculo) {
      return '';
    }
    return `${vehiculo.placa} está deshabilitado por: `
      + `${vehiculo.motivoBloqueo || 'sin motivo registrado'}. `
      + 'Al habilitarlo, vuelve a aparecer en la portería.';
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

  confirmarHabilitar(): void {
    const vehiculo = this.habilitando;
    if (!vehiculo) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('vehiculos', vehiculo.id).subscribe({
      next: () => {
        this.habilitando = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos levantar el bloqueo.';
        this.habilitando = null;
      }
    });
  }

  // ── Autofiltro ───────────────────────────────────────────────────────────

  /** Recalcula la lista visible. Se llama tras cargar y tras cada cambio. */
  filtrar(): void {
    this.visibles = this.filtro.aplicar(this.vehiculos);
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activo(vehiculo: Vehiculo): boolean {
    return vehiculo.activo === 'S';
  }

  bloqueado(vehiculo: Vehiculo): boolean {
    return vehiculo.bloqueado === 'S';
  }

  /**
   * Las dos llaves, y la de la administración se nombra primero porque es la
   * que el hogar no puede levantar: decir "Inactivo" cuando además está
   * deshabilitado mandaría al residente a activarlo desde su celular para
   * nada.
   */
  estado(vehiculo: Vehiculo): string {
    if (this.bloqueado(vehiculo)) {
      return 'Deshabilitado';
    }
    return this.activo(vehiculo) ? 'Activo' : 'Inactivo';
  }

  operativo(vehiculo: Vehiculo): boolean {
    return this.activo(vehiculo) && !this.bloqueado(vehiculo);
  }
}
