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

  /**
   * El encabezado tiene subtítulo SIEMPRE. Sin él, el caso voluntario deja un
   * título huérfano y el bloque pierde el peso frente a los tres campos; y el
   * texto voluntario responde la duda real de esa pantalla, que es por qué se
   * pide el PIN actual.
   */
  get subtitulo(): string {
    return this.obligatorio
      ? 'Estás usando el PIN que te entregaron. Elige uno tuyo.'
      : 'Necesitas tu PIN actual para confirmar el cambio.';
  }

  // ── Mensajes de error ──────────────────────────────────────────────
  //
  // Elegir QUÉ regla se rompió es lógica de negocio y CLAUDE.md §2 la prohíbe
  // en la plantilla. Los tres getters devuelven el texto ya resuelto y la
  // plantilla solo lo pinta; además son la misma fuente que enciende el estado
  // visual del campo, así que color y mensaje no pueden desincronizarse.

  get errorClaveActual(): string | null {
    return this.campoInvalido('claveActual') ? 'Escribe tu PIN actual' : null;
  }

  get errorClaveNueva(): string | null {
    if (!this.campoInvalido('claveNueva')) {
      return null;
    }
    return this.formulario.controls.claveNueva.hasError('pinTrivial')
      ? 'Ese PIN es muy fácil de adivinar. Evita repetidos y seguidos.'
      : 'El PIN son 4 números';
  }

  /**
   * Cubre los DOS fallos posibles. Hasta ahora la plantilla solo miraba
   * `noCoinciden`, así que dejar la confirmación vacía no mostraba nada: el
   * formulario no enviaba y la persona no sabía por qué.
   */
  get errorConfirmacion(): string | null {
    if (this.noCoinciden) {
      return 'Los PIN no coinciden';
    }
    return this.campoInvalido('confirmacion') ? 'Repite el PIN nuevo' : null;
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
   * Cerrar sesión, la única salida cuando el cambio está pendiente: ninguna
   * otra pantalla abre hasta que el PIN se cambie. Quien entró por su cuenta
   * desde el menú no ve este botón sino el enlace de vuelta a su panel —
   * cerrarle la sesión por arrepentirse sería un castigo.
   */
  salir(): void {
    this.auth.cerrarSesion();
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
