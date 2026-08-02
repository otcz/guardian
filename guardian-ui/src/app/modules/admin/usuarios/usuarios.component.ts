import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { Parametro, Persona, Usuario } from '../../../core/models/admin.model';

/**
 * Cuentas de acceso: guardias, administradores y residentes con app. Habilitar
 * o inhabilitar un guardia pasa por acá.
 */
@Component({
  selector: 'gd-admin-usuarios',
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.scss',
  standalone: false
})
export class UsuariosComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  usuarios: Usuario[] = [];
  roles: Parametro[] = [];
  personasSinCuenta: Persona[] = [];

  cargando = true;
  guardando = false;
  error: string | null = null;
  aviso: string | null = null;
  mostrarAlta = false;

  readonly formulario = this.fb.nonNullable.group({
    personaId: [null as number | null, [Validators.required]],
    rol: ['', [Validators.required]]
  });

  constructor(
    private readonly admin: AdminService,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.admin.parametros('ROL').subscribe(roles => (this.roles = roles));
  }

  cargar(): void {
    this.cargando = true;
    this.admin.usuarios().subscribe({
      next: usuarios => {
        this.usuarios = usuarios;
        this.cargando = false;
        this.cargarPersonasSinCuenta();
      },
      error: () => {
        this.error = 'No pudimos cargar los usuarios.';
        this.cargando = false;
      }
    });
  }

  private cargarPersonasSinCuenta(): void {
    this.admin.personas(undefined, 0, 200).subscribe(pagina => {
      const conCuenta = new Set(this.usuarios.map(u => u.personaId));
      this.personasSinCuenta = pagina.content.filter(p => !conCuenta.has(p.id));
    });
  }

  crear(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { personaId, rol } = this.formulario.getRawValue();
    this.guardando = true;
    this.error = null;
    this.aviso = null;

    this.admin.crearUsuario(personaId!, rol).subscribe({
      next: creado => {
        this.guardando = false;
        this.mostrarAlta = false;
        this.formulario.reset();
        this.aviso = `Cuenta creada para ${creado.nombreCompleto}. `
          + 'Nace inactiva: habilítala cuando entregues el acceso. '
          + `La clave inicial es el documento (${creado.documento}).`;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos crear la cuenta.';
      }
    });
  }

  alternarEstado(usuario: Usuario): void {
    this.error = null;
    this.admin.cambiarEstadoUsuario(usuario.id, usuario.activo !== 'S').subscribe({
      next: actualizado => {
        this.usuarios = this.usuarios.map(u => (u.id === actualizado.id ? actualizado : u));
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  /**
   * El backend impide cambiarse el propio rol; el select se deshabilita para
   * no ofrecer una accion que va a fallar.
   */
  esMiCuenta(usuario: Usuario): boolean {
    return this.auth.sesion?.usuarioId === usuario.id;
  }

  cambiarRol(usuario: Usuario, rol: string): void {
    if (rol === usuario.rol) {
      return;
    }
    const seguro = window.confirm(
      `¿Cambiar el rol de ${usuario.nombreCompleto} a ${rol}?`);
    if (!seguro) {
      this.cargar();
      return;
    }

    this.error = null;
    this.admin.cambiarRolUsuario(usuario.id, rol).subscribe({
      next: actualizado => {
        this.usuarios = this.usuarios.map(u => (u.id === actualizado.id ? actualizado : u));
        this.aviso = `${actualizado.nombreCompleto} ahora es ${actualizado.rol}.`;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el rol.';
        this.cargar();
      }
    });
  }

  restablecerClave(usuario: Usuario): void {
    const seguro = window.confirm(
      `¿Restablecer la contraseña de ${usuario.nombreCompleto}? ` +
      'Volverá a ser su documento y deberá cambiarla al entrar.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.restablecerClave(usuario.id).subscribe({
      next: () => {
        this.aviso = `Contraseña de ${usuario.nombreCompleto} restablecida a su documento.`;
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos restablecer la contraseña.';
      }
    });
  }

  // ── Bloqueo administrativo ───────────────────────────────────────────────

  /** Cuenta a la que se le va a poner el candado. Null = hoja cerrada. */
  bloqueando: Usuario | null = null;

  alternarBloqueo(usuario: Usuario): void {
    if (this.bloqueado(usuario)) {
      this.desbloquear(usuario);
    } else {
      this.bloqueando = usuario;
    }
  }

  confirmarBloqueo(motivo: string): void {
    const usuario = this.bloqueando;
    if (!usuario) {
      return;
    }

    this.error = null;
    this.aviso = null;
    this.admin.bloquear('usuarios', usuario.id, motivo).subscribe({
      next: () => {
        this.bloqueando = null;
        this.aviso = `${usuario.nombreCompleto} quedó bloqueado. `
          + 'Si tenía la aplicación abierta, ya salió de ella.';
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos bloquear la cuenta.';
        this.bloqueando = null;
      }
    });
  }

  private desbloquear(usuario: Usuario): void {
    const seguro = window.confirm(
      `${usuario.nombreCompleto} está bloqueado por: ` +
      `${usuario.motivoBloqueo || 'sin motivo registrado'}.\n\n` +
      '¿Levantar el bloqueo? Podrá volver a entrar si su cuenta está habilitada.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('usuarios', usuario.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos levantar el bloqueo.';
      }
    });
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activo(usuario: Usuario): boolean {
    return usuario.activo === 'S';
  }

  bloqueado(usuario: Usuario): boolean {
    return usuario.bloqueado === 'S';
  }

  /** El bloqueo gana: una cuenta bloqueada no entra aunque esté habilitada. */
  estado(usuario: Usuario): string {
    if (this.bloqueado(usuario)) {
      return 'Bloqueado';
    }
    return this.activo(usuario) ? 'Habilitado' : 'Inhabilitado';
  }

  operativo(usuario: Usuario): boolean {
    return this.activo(usuario) && !this.bloqueado(usuario);
  }
}
