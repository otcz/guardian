import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminService } from '../../../core/services/admin.service';
import { Casa, Parametro, Persona } from '../../../core/models/admin.model';

@Component({
  selector: 'gd-personas',
  templateUrl: './personas.component.html',
  styleUrl: './personas.component.scss',
  standalone: false
})
export class PersonasComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  personas: Persona[] = [];
  casas: Casa[] = [];
  parentescos: Parametro[] = [];
  roles: Parametro[] = [];

  cargando = true;
  guardando = false;
  error: string | null = null;
  mostrarAlta = false;

  /**
   * Persona en edición. Null = alta. En edición no se toca la cuenta de
   * acceso: rol y estado de la cuenta se administran en el panel de Usuarios.
   */
  editando: Persona | null = null;

  texto = '';
  private readonly busqueda$ = new Subject<string>();

  readonly formulario = this.fb.nonNullable.group({
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

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();

    this.admin.casas().subscribe(casas => (this.casas = casas));
    this.admin.parametros('PARENTESCO').subscribe(p => (this.parentescos = p));
    this.admin.parametros('ROL').subscribe(r => (this.roles = r));

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
    this.formulario.reset({ fotoUrl: null });
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

  activa(persona: Persona): boolean {
    return persona.activo === 'S';
  }

  /** El parentesco solo aplica si la persona vive en una casa. */
  get pideParentesco(): boolean {
    return !!this.formulario.controls.casaId.value;
  }
}
