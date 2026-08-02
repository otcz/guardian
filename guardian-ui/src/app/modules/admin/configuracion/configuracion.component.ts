import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { GrupoParametro, Parametro } from '../../../core/models/admin.model';

/**
 * Los catálogos que el administrador puede ajustar sin esperar un despliegue:
 * tipos de vehículo, marcas, colores, parentescos, tipos de documento.
 *
 * Dos columnas: a la izquierda los grupos, a la derecha las opciones del que
 * esté seleccionado. Es el mismo gesto de un explorador de archivos, y evita
 * que el administrador tenga que navegar a otra pantalla por cada catálogo.
 */
@Component({
  selector: 'gd-admin-configuracion',
  templateUrl: './configuracion.component.html',
  styleUrl: './configuracion.component.scss',
  standalone: false
})
export class ConfiguracionComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  grupos: GrupoParametro[] = [];
  seleccionado: GrupoParametro | null = null;
  opciones: Parametro[] = [];

  cargando = true;
  cargandoOpciones = false;
  guardando = false;
  error: string | null = null;

  /** Opción en edición. Null = la hoja está en modo alta. */
  editando: Parametro | null = null;
  mostrarHoja = false;

  readonly formulario = this.fb.nonNullable.group({
    valor: ['', [Validators.required, Validators.maxLength(100)]]
  });

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargarGrupos();
  }

  cargarGrupos(): void {
    this.cargando = true;
    this.error = null;
    this.admin.grupos().subscribe({
      next: grupos => {
        this.grupos = grupos;
        this.cargando = false;
        // Abre el primero: una pantalla que arranca vacía obliga a un clic
        // que no decide nada.
        if (!this.seleccionado && grupos.length > 0) {
          this.seleccionar(grupos[0]);
        }
      },
      error: () => {
        this.error = 'No pudimos cargar la configuración.';
        this.cargando = false;
      }
    });
  }

  seleccionar(grupo: GrupoParametro): void {
    this.seleccionado = grupo;
    this.cerrarHoja();
    this.cargarOpciones();
  }

  cargarOpciones(): void {
    if (!this.seleccionado) {
      return;
    }
    this.cargandoOpciones = true;
    this.error = null;
    this.admin.opcionesDelGrupo(this.seleccionado.grupo).subscribe({
      next: opciones => {
        this.opciones = opciones;
        this.cargandoOpciones = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las opciones.';
        this.cargandoOpciones = false;
      }
    });
  }

  // ── Alta y renombrado ────────────────────────────────────────────────────

  abrirAlta(): void {
    this.editando = null;
    this.formulario.reset({ valor: '' });
    this.mostrarHoja = true;
  }

  abrirEdicion(opcion: Parametro): void {
    this.editando = opcion;
    this.formulario.reset({ valor: opcion.valor });
    this.mostrarHoja = true;
  }

  cerrarHoja(): void {
    this.mostrarHoja = false;
    this.editando = null;
    this.formulario.reset({ valor: '' });
  }

  guardar(): void {
    if (this.formulario.invalid || this.guardando || !this.seleccionado) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valor = this.formulario.getRawValue().valor.trim();
    this.guardando = true;
    this.error = null;

    const peticion = this.editando
      ? this.admin.renombrarOpcion(this.editando.id, valor)
      : this.admin.crearOpcion(this.seleccionado.grupo, valor);

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarHoja();
        this.cargarOpciones();
        this.refrescarConteo();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar la opción.';
      }
    });
  }

  alternarEstado(opcion: Parametro): void {
    this.error = null;
    this.admin.cambiarEstadoOpcion(opcion.id, !opcion.activo).subscribe({
      next: actualizada => {
        this.opciones = this.opciones.map(o => (o.id === actualizada.id ? actualizada : o));
        this.refrescarConteo();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  get tituloHoja(): string {
    return this.editando ? 'Cambiar el nombre' : 'Nueva opción';
  }

  /**
   * El código no se toca nunca: hay filas guardadas apuntando a él. Por eso al
   * editar solo se ofrece el nombre, y conviene decir cuál es el código para
   * que nadie espere que cambie.
   */
  get subtituloHoja(): string {
    if (this.editando) {
      return `Código ${this.editando.codigo} · no cambia`;
    }
    return this.seleccionado?.nombre ?? '';
  }

  /** Solo las inactivas necesitan explicación; las activas se ven en la lista. */
  get inactivas(): number {
    return this.opciones.filter(o => !o.activo).length;
  }

  puedeDesactivar(opcion: Parametro): boolean {
    return opcion.activo && !opcion.protegido && !!this.seleccionado?.ampliable;
  }

  motivoNoDesactivable(opcion: Parametro): string {
    if (opcion.protegido) {
      return 'La usa el sistema';
    }
    return 'Este grupo no admite quitar opciones';
  }

  /** El contador del panel izquierdo se vería viejo hasta recargar la página. */
  private refrescarConteo(): void {
    this.admin.grupos().subscribe(grupos => {
      this.grupos = grupos;
      const vigente = grupos.find(g => g.grupo === this.seleccionado?.grupo);
      if (vigente) {
        this.seleccionado = vigente;
      }
    });
  }
}
