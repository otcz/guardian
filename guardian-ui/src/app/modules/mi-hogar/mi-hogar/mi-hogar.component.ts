import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResidenteService } from '../../../core/services/residente.service';
import { AdminService } from '../../../core/services/admin.service';
import {
  CodigoHogar,
  Familiar,
  Parametro,
  Vehiculo
} from '../../../core/models/admin.model';

/**
 * Autogestión del residente: su núcleo familiar (esposa, hijos, invitados) y
 * los vehículos de su casa. Aquí no existe eliminar — solo activar y desactivar;
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

  /**
   * Solo el titular arma el núcleo. Se resuelve ACÁ y no esperando el 403:
   * dejar que cualquiera abra el formulario, lo llene entero y recién ahí
   * recibir "no puedes" es la peor forma de comunicar una regla.
   */
  get esTitular(): boolean {
    return this.familia.some(f => f.esUsuarioActual && f.parentesco === 'TITULAR');
  }

  /** A quién hay que pedírselo. Sin el nombre, el aviso deja sin salida. */
  get nombreDelTitular(): string | null {
    return this.familia.find(f => f.parentesco === 'TITULAR')?.nombreCompleto ?? null;
  }
  vehiculos: Vehiculo[] = [];
  parentescos: Parametro[] = [];
  tiposVehiculo: Parametro[] = [];
  marcasVehiculo: Parametro[] = [];
  coloresVehiculo: Parametro[] = [];
  tiposDocumento: Parametro[] = [];

  cargando = true;
  sinCasa = false;
  error: string | null = null;

  mostrarAltaFamiliar = false;
  mostrarAltaVehiculo = false;
  guardando = false;

  /**
   * Acción destructiva esperando confirmación. Desactivar a un familiar le
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
    this.admin.parametros('MARCA_VEHICULO').subscribe(m => (this.marcasVehiculo = m));
    this.admin.parametros('COLOR_VEHICULO').subscribe(c => (this.coloresVehiculo = c));
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
    // Reactivar no destruye nada: se ejecuta directo. Desactivar sí.
    if (!this.activo(familiar.activo)) {
      this.aplicarEstadoFamiliar(familiar);
      return;
    }
    this.aConfirmar = {
      titulo: familiar.nombreCompleto,
      detalle: 'No podrá entrar al conjunto hasta que lo actives de nuevo.',
      etiqueta: 'Desactivar',
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
      etiqueta: 'Desactivar',
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
   * Deshabilitado por la administración: la otra llave. El titular activa y
   * desactiva lo suyo, pero esta no la puede levantar, así que en su lugar ve
   * un candado y no un interruptor — ofrecerle un botón que el backend le va a
   * negar es prometerle algo que no se cumple.
   */
  bloqueado(entidad: { bloqueado: string }): boolean {
    return entidad.bloqueado === 'S';
  }

  // ── Invitar a alguien al hogar ───────────────────────────────────────────
  //
  // La otra vía para armar un núcleo: en vez de que el titular digite a cada
  // familiar, le pasa un enlace y cada uno crea su propia cuenta.

  codigoHogar: CodigoHogar | null = null;
  mostrarInvitacion = false;
  copiado = false;

  abrirInvitacion(): void {
    this.mostrarInvitacion = true;
    this.copiado = false;
    this.residente.codigoHogar().subscribe({
      next: codigo => (this.codigoHogar = codigo),
      error: () => (this.codigoHogar = null)
    });
  }

  generarCodigo(): void {
    if (this.guardando) {
      return;
    }
    this.guardando = true;
    this.error = null;
    this.copiado = false;

    this.residente.generarCodigoHogar().subscribe({
      next: codigo => {
        this.guardando = false;
        this.codigoHogar = codigo;
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos generar el código.';
      }
    });
  }

  revocarCodigo(): void {
    const seguro = window.confirm(
      '¿Anular el enlace? Quien lo tenga dejará de poder registrarse con él.');
    if (!seguro) {
      return;
    }

    this.residente.revocarCodigoHogar().subscribe({
      next: () => (this.codigoHogar = null),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos anular el código.';
      }
    });
  }

  /** El enlace completo: es lo que se pega en WhatsApp, no el código suelto. */
  get enlaceInvitacion(): string {
    return this.codigoHogar
      ? `${window.location.origin}/unirme/${this.codigoHogar.codigo}`
      : '';
  }

  copiarEnlace(): void {
    navigator.clipboard?.writeText(this.enlaceInvitacion)
      .then(() => (this.copiado = true))
      .catch(() => undefined);
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
        ? `La administración lo deshabilitó: ${motivo}. Comunícate con ella para habilitarlo.`
        : 'La administración lo deshabilitó. Comunícate con ella para habilitarlo.'
    };
  }
}
