import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { GuardiaPorteria, Porteria } from '../../../core/models/admin.model';

/**
 * Las porterías del conjunto: por dónde se entra y se sale.
 *
 * <p>Se administran acá y no en Configuración porque no son un catálogo de
 * etiquetas: cada evento de la bitácora apunta a una de estas filas, y el
 * guardia elige la suya al empezar el turno.</p>
 *
 * <p>Sin eliminar, por lo mismo. Borrar una portería dejaría meses de registros
 * sin poder decir por dónde entró nadie, que es justo lo que la bitácora existe
 * para responder.</p>
 */
@Component({
  selector: 'gd-porterias',
  templateUrl: './porterias.component.html',
  styleUrl: './porterias.component.scss',
  standalone: false
})
export class PorteriasComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  porterias: Porteria[] = [];
  cargando = true;
  guardando = false;
  error: string | null = null;

  /** Portería en edición. Null = el formulario está en modo alta. */
  editando: Porteria | null = null;

  readonly formulario = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    direccion: ['', [Validators.maxLength(160)]],
    // Marcada por defecto: lo común es una portería por donde entra todo, y
    // desmarcarla es la excepción de la puerta peatonal.
    permiteVehiculo: [true]
  });

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;
    this.admin.porterias().subscribe({
      next: porterias => {
        this.porterias = porterias;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las porterías.';
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
    const valores = this.formulario.getRawValue();
    const datos = {
      nombre: valores.nombre,
      direccion: valores.direccion,
      permiteVehiculo: valores.permiteVehiculo ? 'S' : 'N'
    };

    const peticion = this.editando
      ? this.admin.actualizarPorteria(this.editando.id, datos)
      : this.admin.crearPorteria(datos);

    peticion.subscribe({
      next: porteria => {
        this.porterias = (this.editando
          ? this.porterias.map(p => (p.id === porteria.id ? porteria : p))
          : [...this.porterias, porteria])
          .sort((a, b) => a.nombre.localeCompare(b.nombre));
        this.cancelarEdicion();
        this.guardando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar la portería.';
        this.guardando = false;
      }
    });
  }

  editar(porteria: Porteria): void {
    this.editando = porteria;
    this.formulario.setValue({
      nombre: porteria.nombre,
      direccion: porteria.direccion ?? '',
      permiteVehiculo: porteria.permiteVehiculo === 'S'
    });
  }

  cancelarEdicion(): void {
    this.editando = null;
    this.formulario.reset({ nombre: '', direccion: '', permiteVehiculo: true });
  }

  alternarEstado(porteria: Porteria): void {
    const activar = !this.activa(porteria);
    this.error = null;

    this.admin.cambiarEstadoPorteria(porteria.id, activar).subscribe({
      next: actualizada => {
        this.porterias = this.porterias.map(p => (p.id === actualizada.id ? actualizada : p));
      },
      error: (fallo: HttpErrorResponse) => {
        // El backend rechaza apagar la última activa. El mensaje viene de allá
        // porque la regla vive allá: repetirla acá sería tener dos verdades.
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  // ── Estado de pantalla ───────────────────────────────────────────────────

  activa(porteria: Porteria): boolean {
    return porteria.activo === 'S';
  }

  permiteVehiculos(porteria: Porteria): boolean {
    return porteria.permiteVehiculo === 'S';
  }

  campoInvalido(nombre: 'nombre' | 'direccion'): boolean {
    const campo = this.formulario.controls[nombre];
    return campo.invalid && campo.touched;
  }

  // ── Guardias asignados ───────────────────────────────────────────────────
  //
  // La asignación es el valor POR DEFECTO de lo que la tablet le propone al
  // guardia, NO un candado: puede operar una puerta que no es la suya y queda
  // registrado. Impedírselo dejaría gente esperando a las dos de la mañana.

  /** Portería cuya lista de guardias está abierta. Null = hoja cerrada. */
  asignando: Porteria | null = null;

  /**
   * A partir de cuántos guardias aparece el buscador. Con cuatro nombres a la
   * vista es un campo que estorba; con treinta, sin él no se encuentra a nadie.
   */
  readonly UMBRAL_BUSCADOR = 8;

  guardias: GuardiaPorteria[] = [];

  /** Lo que la hoja pinta: los guardias que pasan el filtro del buscador. */
  guardiasVisibles: GuardiaPorteria[] = [];

  busquedaGuardia = '';
  cargandoGuardias = false;

  /** Marcados en la hoja. Un Set porque la pregunta es siempre "¿está?". */
  private readonly marcados = new Set<number>();

  abrirGuardias(porteria: Porteria): void {
    this.asignando = porteria;
    this.guardias = [];
    this.guardiasVisibles = [];
    this.busquedaGuardia = '';
    this.marcados.clear();
    this.cargandoGuardias = true;
    this.error = null;

    this.admin.guardiasDePorteria(porteria.id).subscribe({
      next: guardias => {
        guardias.filter(g => g.asignado).forEach(g => this.marcados.add(g.personaId));

        // Los ya asignados arriba, y dentro de cada grupo el orden por apellido
        // que trae el backend (el sort de JS es estable). Se ordena UNA vez, al
        // abrir: si la lista se reacomodara al marcar, el siguiente nombre
        // saltaría bajo el dedo y se marcaría a quien no era.
        this.guardias = [...guardias]
          .sort((a, b) => Number(b.asignado) - Number(a.asignado));
        this.guardiasVisibles = this.guardias;
        this.cargandoGuardias = false;
      },
      error: () => {
        this.error = 'No pudimos cargar los guardias.';
        this.cargandoGuardias = false;
        this.asignando = null;
      }
    });
  }

  /**
   * Filtra por nombre o documento. Sin tildes a lado y lado: quien busca
   * "yajaira" desde una tablet no escribe la tilde, y no encontrarla lo hace
   * concluir que la persona no está registrada.
   */
  filtrarGuardias(): void {
    const busqueda = this.sinTildes(this.busquedaGuardia).trim();
    if (!busqueda) {
      this.guardiasVisibles = this.guardias;
      return;
    }
    this.guardiasVisibles = this.guardias.filter(g =>
      this.sinTildes(g.nombreCompleto).includes(busqueda)
      || g.documento.toLowerCase().includes(busqueda));
  }

  marcado(guardia: GuardiaPorteria): boolean {
    return this.marcados.has(guardia.personaId);
  }

  alternarMarcado(guardia: GuardiaPorteria): void {
    if (this.marcados.has(guardia.personaId)) {
      this.marcados.delete(guardia.personaId);
    } else {
      this.marcados.add(guardia.personaId);
    }
  }

  get cuantosMarcados(): number {
    return this.marcados.size;
  }

  private sinTildes(texto: string): string {
    return texto.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '');
  }

  guardarGuardias(): void {
    const porteria = this.asignando;
    if (!porteria || this.guardando) {
      return;
    }

    this.guardando = true;
    this.error = null;

    this.admin.asignarGuardias(porteria.id, [...this.marcados]).subscribe({
      next: actualizada => {
        this.porterias = this.porterias.map(p => (p.id === actualizada.id ? actualizada : p));
        this.asignando = null;
        this.guardando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar los guardias.';
        this.guardando = false;
      }
    });
  }
}
