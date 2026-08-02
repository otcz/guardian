import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { CLAVE_INICIAL, Casa, Parametro, Persona } from '../../../core/models/admin.model';

@Component({
  selector: 'gd-personas',
  templateUrl: './personas.component.html',
  styleUrl: './personas.component.scss',
  standalone: false
})
export class PersonasComponent implements OnInit {

  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  personas: Persona[] = [];
  casas: Casa[] = [];
  parentescos: Parametro[] = [];
  roles: Parametro[] = [];
  tiposDocumento: Parametro[] = [];

  cargando = true;
  guardando = false;
  error: string | null = null;
  mostrarAlta = false;

  /** Persona en edición. Null = alta. */
  editando: Persona | null = null;

  texto = '';
  private readonly busqueda$ = new Subject<string>();

  readonly formulario = this.fb.nonNullable.group({
    tipoDocumento: ['CC', [Validators.required]],
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    fechaNacimiento: [''],
    fotoUrl: [null as string | null],
    telefono: [''],
    email: ['', [Validators.email]],
    casaId: [null as number | null],
    parentesco: [''],
    rolUsuario: ['']
  });

  /**
   * Borrar es del operador de la plataforma y de nadie más. El administrador
   * de la sede desactiva, bloquea y revoca — todo eso deja rastro. Borrar no:
   * la fila desaparece y con ella el registro que podría resolver una disputa.
   * El backend lo exige igual; esto solo evita ofrecer un botón que va a
   * responder 403.
   */
  readonly puedeEliminar = this.auth.tieneRol('SUPER_ADMIN');

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();

    this.admin.casas().subscribe(casas => (this.casas = casas));
    this.admin.parametros('PARENTESCO').subscribe(p => (this.parentescos = p));
    this.admin.parametros('ROL').subscribe(r => (this.roles = r));
    this.admin.parametros('TIPO_DOCUMENTO').subscribe(t => (this.tiposDocumento = t));

    // debounce para no disparar una consulta por cada tecla.
    this.busqueda$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(texto => this.cargar(texto));
  }

  buscar(texto: string): void {
    this.texto = texto;
    this.busqueda$.next(texto);
  }

  cargar(texto = ''): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;
    this.admin.personas(texto).subscribe({
      next: pagina => {
        this.personas = pagina.content;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las personas.';
        this.cargando = false;
      }
    });
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
      ...datos,
      fechaNacimiento: datos.fechaNacimiento || null,
      telefono: datos.telefono || null,
      email: datos.email || null,
      casaId: datos.casaId || null,
      parentesco: datos.casaId ? datos.parentesco : null,
      // La cuenta solo se crea en el alta; en edición se administra en Usuarios.
      rolUsuario: this.editando ? null : (datos.rolUsuario || null)
    };

    // El alta devuelve PersonaRegistrada (con el payload del QR) y la
    // edicion una Persona; aqui solo importa que la operacion termine.
    const peticion: Observable<unknown> = this.editando
      ? this.admin.actualizarPersona(this.editando.id, request)
      : this.admin.crearPersona(request);

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cancelarEdicion();
        this.cargar(this.texto);
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar a la persona.';
        this.guardando = false;
      }
    });
  }

  editar(persona: Persona): void {
    this.editando = persona;
    this.mostrarAlta = true;
    this.formulario.setValue({
      tipoDocumento: persona.tipoDocumento ?? 'CC',
      documento: persona.documento,
      nombres: persona.nombres,
      apellidos: persona.apellidos,
      // El API entrega un timestamp; el input date solo entiende yyyy-MM-dd.
      fechaNacimiento: persona.fechaNacimiento?.substring(0, 10) ?? '',
      fotoUrl: persona.fotoUrl,
      telefono: persona.telefono ?? '',
      email: persona.email ?? '',
      casaId: persona.casaId,
      parentesco: persona.parentesco ?? '',
      rolUsuario: ''
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
    this.mostrarAlta = false;
    this.formulario.reset({ tipoDocumento: 'CC', fotoUrl: null });
  }

  alternarEstado(persona: Persona): void {
    this.error = null;
    this.admin.cambiarEstadoPersona(persona.id, persona.activo !== 'S').subscribe({
      next: actualizada => {
        this.personas = this.personas.map(p => (p.id === actualizada.id ? actualizada : p));
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  emitirCredencial(persona: Persona): void {
    this.error = null;
    this.admin.emitirCredencial(persona.id).subscribe({
      next: () => this.cargar(this.texto),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos emitir la credencial.';
      }
    });
  }

  /** Freno de emergencia para un QR comprometido: revoca sin reemitir. */
  revocarCredencial(persona: Persona): void {
    const seguro = window.confirm(
      `¿Revocar el QR de ${persona.nombreCompleto}? Dejará de servir en el próximo escaneo.`);
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.revocarCredencial(persona.id).subscribe({
      next: () => this.cargar(this.texto),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos revocar la credencial.';
      }
    });
  }

  eliminar(persona: Persona): void {
    const seguro = window.confirm(
      `¿Eliminar definitivamente a ${persona.nombreCompleto}? ` +
      'Se borran su cuenta, sus credenciales y su vínculo con la casa. ' +
      'La bitácora conservará sus registros de acceso.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.eliminarPersona(persona.id).subscribe({
      next: () => this.cargar(this.texto),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos eliminar a la persona.';
      }
    });
  }

  // ── Bloqueo administrativo ───────────────────────────────────────────────

  /** Persona a la que se le va a poner el candado. Null = hoja cerrada. */
  bloqueando: Persona | null = null;

  alternarBloqueo(persona: Persona): void {
    if (this.bloqueada(persona)) {
      this.desbloquear(persona);
    } else {
      this.bloqueando = persona;
    }
  }

  confirmarBloqueo(motivo: string): void {
    const persona = this.bloqueando;
    if (!persona) {
      return;
    }

    this.error = null;
    this.admin.bloquear('personas', persona.id, motivo).subscribe({
      next: () => {
        this.bloqueando = null;
        this.cargar(this.texto);
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos bloquear a la persona.';
        this.bloqueando = null;
      }
    });
  }

  private desbloquear(persona: Persona): void {
    const seguro = window.confirm(
      `${persona.nombreCompleto} está deshabilitada por: ` +
      `${persona.motivoBloqueo || 'sin motivo registrado'}.\n\n` +
      '¿Habilitarla de nuevo? Volverá a ingresar si su hogar la tiene activa.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('personas', persona.id).subscribe({
      next: () => this.cargar(this.texto),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos habilitarla.';
      }
    });
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activa(persona: Persona): boolean {
    return persona.activo === 'S';
  }

  /**
   * Tiene cuenta de acceso pero nadie la ha habilitado todavía, así que no
   * puede entrar por más que su registro figure activo.
   */
  cuentaInactiva(persona: Persona): boolean {
    return this.tieneCuenta(persona) && persona.usuarioActivo === 'N';
  }

  // ── Acceso a la aplicación ───────────────────────────────────────────────
  //
  // Una persona y su cuenta se administran DESDE EL MISMO SITIO. Antes la
  // cuenta vivía en otro panel, y esa separación —que en la base es correcta—
  // se le aparecía al administrador como dos pantallas que se contradecían.

  /** Persona cuyo acceso se está administrando. Null = hoja cerrada. */
  gestionandoAcceso: Persona | null = null;
  avisoAcceso: string | null = null;

  readonly formularioAcceso = this.fb.nonNullable.group({
    rol: ['', [Validators.required]]
  });

  readonly formularioClave = this.fb.nonNullable.group({
    claveNueva: ['', [Validators.required, Validators.minLength(8)]],
    confirmacion: ['', [Validators.required]]
  });

  /** Sub-hoja para escribir la contraseña. Se abre encima de la de acceso. */
  cambiandoClave = false;

  tieneCuenta(persona: Persona): boolean {
    return persona.usuarioId !== null;
  }

  cuentaBloqueada(persona: Persona): boolean {
    return persona.usuarioBloqueado === 'S';
  }

  /** Lo que se lee en la columna Acceso, en una sola frase. */
  estadoAcceso(persona: Persona): string {
    if (!this.tieneCuenta(persona)) {
      return 'Sin acceso';
    }
    if (this.cuentaBloqueada(persona)) {
      return 'Deshabilitada';
    }
    return persona.usuarioActivo === 'S' ? 'Activa' : 'Inactiva';
  }

  abrirAcceso(persona: Persona): void {
    this.gestionandoAcceso = persona;
    this.avisoAcceso = null;
    this.cambiandoClave = false;
    this.formularioAcceso.setValue({ rol: persona.rol ?? '' });
    this.formularioClave.reset();
  }

  /** Le da acceso a la aplicación a alguien que solo era persona. */
  darAcceso(): void {
    const persona = this.gestionandoAcceso;
    if (!persona || this.formularioAcceso.invalid || this.guardando) {
      this.formularioAcceso.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    this.admin.crearUsuario(persona.id, this.formularioAcceso.getRawValue().rol).subscribe({
      next: () => {
        this.guardando = false;
        this.avisoAcceso = `Ya puede entrar con la contraseña ${this.claveInicial}.`;
        this.refrescarAcceso();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos crear la cuenta.';
      }
    });
  }

  cambiarRol(rol: string): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId || rol === persona.rol) {
      return;
    }

    this.error = null;
    this.admin.cambiarRolUsuario(persona.usuarioId, rol).subscribe({
      next: () => this.refrescarAcceso(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el rol.';
        this.refrescarAcceso();
      }
    });
  }

  alternarEstadoCuenta(): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId) {
      return;
    }

    this.error = null;
    this.admin
      .cambiarEstadoUsuario(persona.usuarioId, persona.usuarioActivo !== 'S')
      .subscribe({
        next: () => this.refrescarAcceso(),
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado de la cuenta.';
        }
      });
  }

  restablecerClave(): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId) {
      return;
    }
    const seguro = window.confirm(
      `¿Devolver la contraseña de ${persona.nombreCompleto} a ${this.claveInicial}? `
      + 'Deberá cambiarla al entrar y su sesión abierta dejará de servirle.');
    if (!seguro) {
      return;
    }

    this.error = null;
    this.admin.restablecerClave(persona.usuarioId).subscribe({
      next: () => {
        this.avisoAcceso = `Contraseña devuelta a ${this.claveInicial}.`;
        this.refrescarAcceso();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos restablecer la contraseña.';
      }
    });
  }

  /** La confirmación evita que un error de tecleo deje al dueño por fuera. */
  get claveNoCoincide(): boolean {
    const { claveNueva, confirmacion } = this.formularioClave.getRawValue();
    return confirmacion.length > 0 && claveNueva !== confirmacion;
  }

  asignarClave(): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId || this.formularioClave.invalid
        || this.claveNoCoincide || this.guardando) {
      this.formularioClave.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    this.admin.asignarClave(persona.usuarioId, this.formularioClave.getRawValue().claveNueva)
      .subscribe({
        next: () => {
          this.guardando = false;
          this.cambiandoClave = false;
          this.formularioClave.reset();
          this.avisoAcceso = 'Contraseña asignada. Deberá cambiarla en su próximo ingreso.';
          this.refrescarAcceso();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos asignar la contraseña.';
        }
      });
  }

  alternarBloqueoCuenta(): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId) {
      return;
    }

    if (this.cuentaBloqueada(persona)) {
      const seguro = window.confirm(
        `La cuenta está deshabilitada por: `
        + `${persona.usuarioMotivoBloqueo || 'sin motivo registrado'}.`
        + '\n\n¿Habilitarla de nuevo?');
      if (!seguro) {
        return;
      }
      this.admin.desbloquear('usuarios', persona.usuarioId).subscribe({
        next: () => this.refrescarAcceso(),
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos habilitar la cuenta.';
        }
      });
      return;
    }
    this.bloqueandoCuenta = true;
  }

  /** Hoja del motivo, para bloquear la CUENTA (no la persona). */
  bloqueandoCuenta = false;

  confirmarBloqueoCuenta(motivo: string): void {
    const persona = this.gestionandoAcceso;
    if (!persona?.usuarioId) {
      return;
    }

    this.error = null;
    this.admin.bloquear('usuarios', persona.usuarioId, motivo).subscribe({
      next: () => {
        this.bloqueandoCuenta = false;
        this.avisoAcceso = 'Cuenta deshabilitada. Si tenía la aplicación abierta, ya salió de ella.';
        this.refrescarAcceso();
      },
      error: (fallo: HttpErrorResponse) => {
        this.bloqueandoCuenta = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos deshabilitar la cuenta.';
      }
    });
  }

  /**
   * Recarga el listado y vuelve a apuntar la hoja a la MISMA persona: sin
   * esto la hoja seguiría mostrando el estado anterior después de cada acción.
   */
  private refrescarAcceso(): void {
    const id = this.gestionandoAcceso?.id;
    this.admin.personas(this.texto).subscribe(pagina => {
      this.personas = pagina.content;
      this.gestionandoAcceso = this.personas.find(p => p.id === id) ?? null;
      if (this.gestionandoAcceso) {
        this.formularioAcceso.setValue({ rol: this.gestionandoAcceso.rol ?? '' });
      }
    });
  }

  readonly claveInicial = CLAVE_INICIAL;

  bloqueada(persona: Persona): boolean {
    return persona.bloqueado === 'S';
  }

  /**
   * La llave de la administración gana sobre la del hogar: es la que el
   * titular no puede levantar. Decir "Inactiva" cuando además está
   * deshabilitada lo mandaría a activarla desde su celular para nada.
   */
  estado(persona: Persona): string {
    if (this.bloqueada(persona)) {
      return 'Deshabilitada';
    }
    return this.activa(persona) ? 'Activa' : 'Inactiva';
  }

  operativa(persona: Persona): boolean {
    return this.activa(persona) && !this.bloqueada(persona);
  }

  /** El parentesco solo aplica si la persona vive en una casa. */
  get pideParentesco(): boolean {
    return !!this.formulario.controls.casaId.value;
  }
}
