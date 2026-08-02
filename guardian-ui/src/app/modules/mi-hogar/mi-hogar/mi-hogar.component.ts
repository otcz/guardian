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
  tiposDocumento: Parametro[] = [];

  cargando = true;
  sinCasa = false;
  error: string | null = null;

  mostrarAltaFamiliar = false;
  mostrarAltaVehiculo = false;
  guardando = false;

  /**
   * Acción destructiva esperando confirmación. Inactivar a un familiar le
   * quita el ingreso al conjunto: no puede pasar por un toque accidental,
   * y hasta ahora no preguntaba nada.
   */
  aConfirmar: { titulo: string; detalle: string; etiqueta: string; accion: () => void } | null = null;

  readonly formularioFamiliar = this.fb.nonNullable.group({
    tipoDocumento: ['CC', [Validators.required]],
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
    this.admin.parametros('TIPO_DOCUMENTO').subscribe(t => (this.tiposDocumento = t));
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;

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
          this.formularioFamiliar.reset({ tipoDocumento: 'CC', fotoUrl: null });
          this.cargar();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar a la persona.';
        }
      });
  }

  alternarEstadoFamiliar(familiar: Familiar): void {
    // Reactivar no destruye nada: se ejecuta directo. Inactivar sí.
    if (!this.activo(familiar.activo)) {
      this.aplicarEstadoFamiliar(familiar);
      return;
    }
    this.aConfirmar = {
      titulo: familiar.nombreCompleto,
      detalle: 'No podrá entrar al conjunto hasta que lo actives de nuevo.',
      etiqueta: 'Inactivar',
      accion: () => this.aplicarEstadoFamiliar(familiar)
    };
  }

  private aplicarEstadoFamiliar(familiar: Familiar): void {
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
    if (!this.activo(vehiculo.activo)) {
      this.aplicarEstadoVehiculo(vehiculo);
      return;
    }
    this.aConfirmar = {
      titulo: vehiculo.placa,
      detalle: 'La portería dejará de permitir su ingreso.',
      etiqueta: 'Inhabilitar',
      accion: () => this.aplicarEstadoVehiculo(vehiculo)
    };
  }

  private aplicarEstadoVehiculo(vehiculo: Vehiculo): void {
    this.error = null;
    this.residente.cambiarEstadoVehiculo(vehiculo.id, vehiculo.activo !== 'S').subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  confirmar(): void {
    const pendiente = this.aConfirmar;
    this.aConfirmar = null;
    pendiente?.accion();
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  etiquetaParentesco(codigo: string): string {
    return this.parentescos.find(p => p.codigo === codigo)?.valor ?? codigo;
  }

  activo(estado: string): boolean {
    return estado === 'S';
  }

  /**
   * Bloqueado por la administración. El residente no puede levantarlo, así que
   * en su lugar ve un candado y no un interruptor: ofrecerle un botón que el
   * backend le va a negar es prometerle algo que no se cumple.
   */
  bloqueado(entidad: { bloqueado: string }): boolean {
    return entidad.bloqueado === 'S';
  }

  /**
   * Explicación del candado. Hoja aparte y no la de confirmar: esa ofrece una
   * acción destructiva en rojo, y acá no hay nada que hacer más que leer.
   */
  bloqueoInfo: { titulo: string; detalle: string } | null = null;

  explicarBloqueo(nombre: string, motivo: string | null): void {
    this.bloqueoInfo = {
      titulo: nombre,
      detalle: motivo
        ? `La administración lo bloqueó: ${motivo}. Comunícate con ella para levantarlo.`
        : 'La administración lo bloqueó. Comunícate con ella para levantarlo.'
    };
  }
}
