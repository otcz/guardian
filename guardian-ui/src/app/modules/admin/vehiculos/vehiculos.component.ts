import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  Casa,
  ImportacionVehiculos,
  Parametro,
  Vehiculo
} from '../../../core/models/admin.model';

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
    color: ['']
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
      `${vehiculo.placa} está deshabilitado por: ` +
      `${vehiculo.motivoBloqueo || 'sin motivo registrado'}.\n\n` +
      '¿Habilitarlo de nuevo? Volverá a aparecer en la portería.');
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

  // ── Carga masiva ─────────────────────────────────────────────────────────

  importando = false;
  resultado: ImportacionVehiculos | null = null;

  /**
   * La plantilla la genera el SERVIDOR: sale con las casas, tipos, marcas y
   * colores reales de esta sede, y con las mismas columnas que lee el
   * importador. Una escrita a mano acá se separa del lector al primer cambio.
   */
  descargarPlantilla(): void {
    this.error = null;
    this.admin.plantillaVehiculos().subscribe({
      next: libro => this.descargar(libro, 'plantilla-vehiculos.xlsx'),
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

    this.admin.importarVehiculos(archivo).subscribe({
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
