import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

import { SedeService } from '../../../core/services/sede.service';
import { AuthService } from '../../../core/services/auth.service';
import { TemaService } from '../../../core/services/tema.service';
import { Sede } from '../../../core/models/sede.model';
import { Sesion } from '../../../core/models/sesion.model';

/**
 * Panel de la plataforma: el mapa de sedes del super administrador.
 *
 * <p>No es un back-office más. Acá no se gestionan casas ni personas: se
 * crean sedes, se les nombra un primer administrador y se entra a una para
 * operarla. Todo lo demás pasa dentro del panel de esa sede.</p>
 */
@Component({
  selector: 'gd-sedes',
  templateUrl: './sedes.component.html',
  styleUrl: './sedes.component.scss',
  standalone: false
})
export class SedesComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  sedes: Sede[] = [];
  sesion: Sesion | null = null;

  cargando = true;
  guardando = false;
  error: string | null = null;

  mostrarAlta = false;
  /** Sede en edición. Null con el formulario abierto = alta. */
  editando: Sede | null = null;

  /** Sede a la que se le está nombrando el primer administrador. */
  nombrandoAdmin: Sede | null = null;

  readonly formulario = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    nit: [''],
    direccion: [''],
    telefono: ['']
  });

  readonly formularioAdmin = this.fb.nonNullable.group({
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    telefono: [''],
    email: ['', [Validators.email]]
  });

  constructor(
    private readonly sedeService: SedeService,
    private readonly auth: AuthService,
    public readonly tema: TemaService,
    private readonly router: Router
  ) {
    this.auth.sesion$.subscribe(sesion => (this.sesion = sesion));
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.sedeService.sedes().subscribe({
      next: sedes => {
        this.sedes = sedes;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las sedes.';
        this.cargando = false;
      }
    });
  }

  // ── Alta y edición ───────────────────────────────────────────────────────

  abrirAlta(): void {
    this.editando = null;
    this.formulario.reset();
    this.mostrarAlta = true;
  }

  editar(sede: Sede): void {
    this.editando = sede;
    this.formulario.setValue({
      nombre: sede.nombre,
      nit: sede.nit ?? '',
      direccion: sede.direccion ?? '',
      telefono: sede.telefono ?? ''
    });
    this.mostrarAlta = true;
  }

  cancelar(): void {
    this.mostrarAlta = false;
    this.editando = null;
    this.formulario.reset();
  }

  guardar(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    const datos = this.formulario.getRawValue();
    const request = {
      nombre: datos.nombre,
      nit: datos.nit || null,
      direccion: datos.direccion || null,
      telefono: datos.telefono || null
    };

    const peticion: Observable<Sede> = this.editando
      ? this.sedeService.actualizar(this.editando.id, request)
      : this.sedeService.crear(request);

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cancelar();
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar la sede.';
        this.guardando = false;
      }
    });
  }

  // ── Primer administrador ─────────────────────────────────────────────────

  abrirAdministrador(sede: Sede): void {
    this.nombrandoAdmin = sede;
    this.formularioAdmin.reset();
  }

  guardarAdministrador(): void {
    if (!this.nombrandoAdmin || this.formularioAdmin.invalid || this.guardando) {
      this.formularioAdmin.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    const datos = this.formularioAdmin.getRawValue();
    this.sedeService.crearAdministrador(this.nombrandoAdmin.id, {
      documento: datos.documento,
      nombres: datos.nombres,
      apellidos: datos.apellidos,
      telefono: datos.telefono || null,
      email: datos.email || null
    }).subscribe({
      next: () => {
        this.guardando = false;
        this.nombrandoAdmin = null;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos crear al administrador.';
        this.guardando = false;
      }
    });
  }

  // ── Estado y suplantación ────────────────────────────────────────────────

  alternarEstado(sede: Sede): void {
    if (this.activa(sede)) {
      const seguro = window.confirm(
        `¿Desactivar ${sede.nombre}? Toda su gente queda sin poder ingresar a la ` +
        'aplicación, incluidos los guardias con sesión abierta.');
      if (!seguro) {
        return;
      }
    }

    this.error = null;
    this.sedeService.cambiarEstado(sede.id, !this.activa(sede)).subscribe({
      next: actualizada => {
        this.sedes = this.sedes.map(s => (s.id === actualizada.id ? actualizada : s));
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  /**
   * Entrar cambia el token: a partir de acá el panel de administración opera
   * sobre esta sede sin que ningún endpoint reciba la sede como parámetro.
   */
  entrar(sede: Sede): void {
    this.error = null;
    this.sedeService.entrar(sede.id).subscribe({
      next: () => this.router.navigate(['/admin']),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos entrar a la sede.';
      }
    });
  }

  salir(): void {
    this.auth.cerrarSesion();
  }

  // ── Ayudas de pantalla ───────────────────────────────────────────────────

  activa(sede: Sede): boolean {
    return sede.activo === 'S';
  }

  /** Documento en vivo, para previsualizar la credencial que se va a crear. */
  get documentoAdmin(): string {
    return this.formularioAdmin.controls.documento.value.trim().toUpperCase();
  }

  /**
   * Totales de la plataforma. Se calculan acá y no en el template: la regla
   * es que el HTML bindea valores, no los deriva.
   *
   * <p>Sin endpoint nuevo — los contadores ya vienen por sede, y sumarlos en
   * el cliente evita una consulta agregada por cada visita a una pantalla que
   * en un despliegue real tiene diez o veinte filas.</p>
   */
  get total(): {
    activas: number; casas: number; personas: number;
    cuentas: number; sinAdministrador: number;
  } {
    return this.sedes.reduce(
      (acumulado, sede) => ({
        activas: acumulado.activas + (this.activa(sede) ? 1 : 0),
        casas: acumulado.casas + sede.casas,
        personas: acumulado.personas + sede.personas,
        cuentas: acumulado.cuentas + sede.usuarios,
        sinAdministrador: acumulado.sinAdministrador + (sede.tieneAdministrador ? 0 : 1)
      }),
      { activas: 0, casas: 0, personas: 0, cuentas: 0, sinAdministrador: 0 });
  }

  get tituloFormulario(): string {
    return this.editando ? 'Editar sede' : 'Nueva sede';
  }
}
