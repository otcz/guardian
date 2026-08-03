import { Component, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';

import { validadorPin } from '../../../core/validadores/pin.validador';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'gd-cambiar-clave',
  templateUrl: './cambiar-clave.component.html',
  styleUrl: './cambiar-clave.component.scss',
  standalone: false
})
export class CambiarClaveComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  readonly formulario = this.fb.nonNullable.group(
    {
      claveActual: ['', [Validators.required]],
      claveNueva: ['', [Validators.required, validadorPin()]],
      confirmacion: ['', [Validators.required]]
    },
    { validators: [coincidenLasClaves] }
  );

  cargando = false;
  error: string | null = null;

  /** Un toggle por campo: ver la actual no debe destapar la nueva. */
  verActual = false;
  verNueva = false;
  verConfirmacion = false;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (!this.auth.autenticado) {
      this.router.navigate(['/ingreso']);
    }
  }

  get obligatorio(): boolean {
    return this.auth.requiereCambioClave;
  }

  guardar(): void {
    if (this.formulario.invalid || this.cargando) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { claveActual, claveNueva } = this.formulario.getRawValue();
    this.cargando = true;
    this.error = null;

    this.auth.cambiarClave({ claveActual, claveNueva }).subscribe({
      next: () => {
        this.cargando = false;
        this.router.navigate([this.auth.rutaInicial()]);
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar el cambio. Intenta de nuevo.';
      }
    });
  }

  /**
   * La salida depende de POR QUE se llegó acá.
   *
   * <p>Con el cambio obligatorio pendiente no hay a dónde volver —ninguna
   * otra pantalla abre— así que la única salida es cerrar sesión. Pero quien
   * entró por su cuenta desde el menú viene de algún lado, y cerrarle la
   * sesión por pulsar "Salir" sería castigarlo por arrepentirse.</p>
   */
  salir(): void {
    if (this.obligatorio) {
      this.auth.cerrarSesion();
      return;
    }
    this.router.navigate([this.auth.rutaInicial()]);
  }

  campoInvalido(nombre: 'claveActual' | 'claveNueva' | 'confirmacion'): boolean {
    const campo = this.formulario.controls[nombre];
    return campo.invalid && campo.touched;
  }

  get noCoinciden(): boolean {
    return this.formulario.hasError('noCoinciden')
        && this.formulario.controls.confirmacion.touched;
  }
}

/** La confirmación tiene que ser idéntica a la clave nueva. */
function coincidenLasClaves(grupo: AbstractControl): ValidationErrors | null {
  const nueva = grupo.get('claveNueva')?.value;
  const confirmacion = grupo.get('confirmacion')?.value;
  return nueva && confirmacion && nueva !== confirmacion ? { noCoinciden: true } : null;
}
