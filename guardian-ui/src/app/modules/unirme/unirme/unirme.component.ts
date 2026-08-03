import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { HogarPublicoService } from '../../../core/services/hogar-publico.service';
import { CLAVE_INICIAL, HogarPublico, Parametro } from '../../../core/models/admin.model';

/**
 * La pantalla de quien recibió el código de su familia.
 *
 * <p>Sin sesión: llega por un enlace y todavía no existe en el sistema. Al
 * terminar tiene cuenta propia y entra con el PIN inicial.</p>
 */
@Component({
  selector: 'gd-unirme',
  templateUrl: './unirme.component.html',
  styleUrl: './unirme.component.scss',
  standalone: false
})
export class UnirmeComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  hogar: HogarPublico | null = null;
  cargando = true;
  guardando = false;
  listo = false;
  error: string | null = null;

  readonly claveInicial = CLAVE_INICIAL;

  private codigo = '';

  readonly formulario = this.fb.nonNullable.group({
    tipoDocumento: ['CC', [Validators.required]],
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    fechaNacimiento: [''],
    telefono: [''],
    // Obligatorio: quien se registra por acá SIEMPRE sale con cuenta, y sin
    // correo esa cuenta nace sin forma de recuperar el PIN.
    email: ['', [Validators.required, Validators.email]],
    parentesco: ['', [Validators.required]]
  });

  constructor(
    private readonly hogarPublico: HogarPublicoService,
    private readonly ruta: ActivatedRoute,
    private readonly router: Router
  ) {}

  // Las opciones llegan CON la respuesta pública: esta pantalla no tiene
  // sesión y no puede pedir el catálogo por su cuenta.
  get parentescos(): Parametro[] {
    return this.hogar?.parentescos ?? [];
  }

  get tiposDocumento(): Parametro[] {
    return this.hogar?.tiposDocumento ?? [];
  }

  ngOnInit(): void {
    this.codigo = this.ruta.snapshot.paramMap.get('codigo') ?? '';

    this.hogarPublico.consultar(this.codigo).subscribe({
      next: hogar => {
        this.hogar = hogar;
        this.cargando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        this.error = fallo.error?.mensaje ?? 'Ese código no es válido.';
      }
    });

  }

  registrarme(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    const datos = this.formulario.getRawValue();
    this.hogarPublico.registrar(this.codigo, {
      ...datos,
      fechaNacimiento: datos.fechaNacimiento || null,
      telefono: datos.telefono || null
    }).subscribe({
      next: () => {
        this.guardando = false;
        this.listo = true;
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos completar tu registro.';
      }
    });
  }

  irAlLogin(): void {
    this.router.navigate(['/ingreso']);
  }

  /** El documento que acaba de escribir: es también su usuario. */
  get miUsuario(): string {
    return this.formulario.controls.documento.value.trim().toUpperCase();
  }
}
