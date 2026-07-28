import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

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

  texto = '';
  private readonly busqueda$ = new Subject<string>();

  readonly formulario = this.fb.nonNullable.group({
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    fechaNacimiento: [''],
    fotoUrl: [null as string | null],
    telefono: [''],
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

  crear(): void {
    if (this.formulario.invalid || this.guardando) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    const datos = this.formulario.getRawValue();

    this.admin
      .crearPersona({
        ...datos,
        fechaNacimiento: datos.fechaNacimiento || null,
        telefono: datos.telefono || null,
        casaId: datos.casaId || null,
        parentesco: datos.casaId ? datos.parentesco : null,
        rolUsuario: datos.rolUsuario || null
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarAlta = false;
          this.formulario.reset({ fotoUrl: null });
          this.cargar(this.texto);
        },
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar a la persona.';
          this.guardando = false;
        }
      });
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
