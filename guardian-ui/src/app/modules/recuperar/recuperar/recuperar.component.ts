import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

type Paso = 'documento' | 'codigo' | 'listo';

/**
 * "Olvidé mi contraseña" en dos pasos, sin salir de la aplicación.
 *
 * <p>Se eligió un código de seis dígitos y no un enlace. GUARDIAN se usa como
 * aplicación instalada en el teléfono: un enlace abriría el navegador y sacaría
 * a la persona de la aplicación, mientras que el código la deja donde estaba.
 * Además es el gesto que esta audiencia ya conoce del banco y de WhatsApp.</p>
 */
@Component({
  selector: 'gd-recuperar',
  templateUrl: './recuperar.component.html',
  styleUrl: './recuperar.component.scss',
  standalone: false
})
export class RecuperarComponent {

  private readonly fb = inject(FormBuilder);

  paso: Paso = 'documento';
  cargando = false;
  error: string | null = null;

  /** Lo que dice el backend, que es siempre lo mismo exista o no la cuenta. */
  aviso: string | null = null;
  minutosVigencia = 10;

  readonly formularioDocumento = this.fb.nonNullable.group({
    documento: ['', [Validators.required]]
  });

  readonly formularioCodigo = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    claveNueva: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]]
  });

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  // ── Paso 1 ───────────────────────────────────────────────────────────────

  solicitar(): void {
    if (this.formularioDocumento.invalid || this.cargando) {
      this.formularioDocumento.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    this.auth.solicitarCodigo(this.formularioDocumento.getRawValue()).subscribe({
      next: respuesta => {
        this.cargando = false;
        this.aviso = respuesta.mensaje;
        this.minutosVigencia = respuesta.minutosVigencia;
        // Se avanza SIEMPRE, exista o no la cuenta. Quedarse en el primer paso
        // cuando el documento no existe delataría cuáles sí existen, que es
        // exactamente lo que el backend se cuida de no decir.
        this.paso = 'codigo';
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos enviar el código. Intenta de nuevo.';
      }
    });
  }

  // ── Paso 2 ───────────────────────────────────────────────────────────────

  restablecer(): void {
    if (this.formularioCodigo.invalid || this.cargando) {
      this.formularioCodigo.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    const datos = this.formularioCodigo.getRawValue();

    this.auth
      .restablecerClave({
        documento: this.formularioDocumento.getRawValue().documento,
        codigo: datos.codigo,
        claveNueva: datos.claveNueva
      })
      .subscribe({
        next: () => {
          this.cargando = false;
          this.paso = 'listo';
        },
        error: (fallo: HttpErrorResponse) => {
          this.cargando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos cambiar la contraseña.';
        }
      });
  }

  /** Volver al paso 1 limpia el código: el anterior ya no sirve. */
  pedirOtro(): void {
    this.formularioCodigo.reset({ codigo: '', claveNueva: '' });
    this.error = null;
    this.aviso = null;
    this.paso = 'documento';
  }

  irAlLogin(): void {
    this.router.navigate(['/']);
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  /**
   * Recibe el control y no el par (formulario, nombre): los dos formularios
   * tienen tipos distintos, y la unión de ambos deja de ser invocable con
   * `.get()`. La plantilla pasa `formularioX.controls.y`, que además es lo que
   * el compilador puede verificar.
   */
  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }
}
