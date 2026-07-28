import { AfterViewInit, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';

const LLAVE_ULTIMO_USUARIO = 'guardian.ultimoUsuario';

@Component({
  selector: 'gd-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  standalone: false
})
export class LoginComponent implements AfterViewInit {

  private readonly fb = inject(FormBuilder);

  readonly formulario = this.fb.nonNullable.group({
    documento: [localStorage.getItem(LLAVE_ULTIMO_USUARIO) ?? '', [Validators.required]],
    clave: ['', [Validators.required]]
  });

  cargando = false;
  error: string | null = null;
  verClave = false;

  /** Prende solo si ya habia un usuario recordado; el resto opta al entrar. */
  recordar = localStorage.getItem(LLAVE_ULTIMO_USUARIO) !== null;

  @ViewChild('campoDocumento')
  private campoDocumento?: ElementRef<HTMLInputElement>;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {}

  ngAfterViewInit(): void {
    // Si el usuario quedo recordado, lo que falta es la clave.
    const campo = this.formulario.controls.documento.value
      ? undefined
      : this.campoDocumento;
    campo?.nativeElement.focus();
  }

  entrar(): void {
    if (this.formulario.invalid || this.cargando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    const credenciales = {
      documento: this.formulario.getRawValue().documento.trim(),
      clave: this.formulario.getRawValue().clave
    };

    this.auth.login(credenciales).subscribe({
      next: respuesta => {
        if (this.recordar) {
          localStorage.setItem(LLAVE_ULTIMO_USUARIO, credenciales.documento);
        } else {
          localStorage.removeItem(LLAVE_ULTIMO_USUARIO);
        }
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
