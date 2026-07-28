import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'gd-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  standalone: false
})
export class LoginComponent {

  private readonly fb = inject(FormBuilder);

  readonly formulario = this.fb.nonNullable.group({
    documento: ['', [Validators.required]],
    clave: ['', [Validators.required]]
  });

  cargando = false;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  entrar(): void {
    if (this.formulario.invalid || this.cargando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    this.auth.login(this.formulario.getRawValue()).subscribe({
      next: respuesta => {
        this.cargando = false;
        this.router.navigate([
          respuesta.requiereCambioClave ? '/cambiar-clave' : this.auth.rutaInicial()
        ]);
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos conectarnos. Intenta de nuevo.';
      }
    });
  }

  campoInvalido(nombre: 'documento' | 'clave'): boolean {
    const campo = this.formulario.controls[nombre];
    return campo.invalid && campo.touched;
  }
}
