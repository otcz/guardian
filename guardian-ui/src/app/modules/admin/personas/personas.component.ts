import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminService } from '../../../core/services/admin.service';
import { FiltroTabla } from '../../../shared/tabla/filtro-tabla';
import { AuthService } from '../../../core/services/auth.service';
import { validadorPin } from '../../../core/validadores/pin.validador';
import {
  CLAVE_INICIAL,
  Casa,
  ImportacionPersonas,
  Parametro,
  Persona
} from '../../../core/models/admin.model';

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

  /** Lo que la tabla pinta: las personas que pasan el autofiltro. */
  visibles: Persona[] = [];

  /**
   * Autofiltro por columna, como en una hoja de cálculo.
   *
   * <p>Sin buscador propio: el de arriba ya pregunta al servidor, que es quien
   * tiene TODAS las personas. Duplicarlo acá filtraría solo lo cargado y diría
   * "no hay nadie" con la persona esperando en el servidor.</p>
   */
  readonly filtro = new FiltroTabla<Persona>(
    {
      rol: p => p.rol ?? 'Sin cuenta',
      casa: p => p.casaIdentificador,
      registro: p => this.estado(p),
      acceso: p => this.estadoAcceso(p)
    },
    () => ''
  );
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
    this.admin.parametros('ROL').subscribe(r => {
      this.roles = this.asignables(r);
      // El default se aplica al LLEGAR el catálogo, no antes: el select no
      // puede seleccionar una opción que todavía no existe en el DOM.
      if (!this.editando && !this.formulario.controls.rolUsuario.value) {
        this.formulario.controls.rolUsuario.setValue(this.rolPorDefecto());
      }
    });
    this.admin.parametros('TIPO_DOCUMENTO').subscribe(t => (this.tiposDocumento = t));

    // debounce para no disparar una consulta por cada tecla.
    this.busqueda$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(texto => this.cargar(texto));

    // Elegir un rol vuelve el correo obligatorio; quitarlo lo suelta. Y elegir
    // GUARDIA retira la casa, que para él no aplica.
    this.formulario.controls.rolUsuario.valueChanges.subscribe(rol => {
      this.sincronizarValidadorDeCorreo();
      this.sincronizarCasaConElRol(rol);
    });

    // Elegir una casa vacía deja el parentesco en TITULAR: el primero que
    // entra tiene que serlo, o esa familia nace sin quien la administre.
    this.formulario.controls.casaId.valueChanges
      .subscribe(casaId => this.sincronizarParentescoConLaCasa(casaId));
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
        this.filtrar();
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
    // Vuelve al rol por defecto y no a vacío: ya no existe "sin cuenta", así
    // que un formulario en blanco dejaría el select sin ninguna opción marcada.
    this.formulario.reset({
      tipoDocumento: 'CC',
      fotoUrl: null,
      rolUsuario: this.rolPorDefecto()
    });
  }

  /**
   * RESIDENTE, que es la inmensa mayoría de un conjunto. Si el administrador lo
   * ocultara desde Configuración, cae al primero que quede: un default que
   * apunta a una opción inexistente dejaría el select en blanco y el alta
   * bloqueada sin decir por qué.
   */
  private rolPorDefecto(): string {
    const residente = this.roles.find(r => r.codigo === 'RESIDENTE');
    return residente?.codigo ?? this.roles[0]?.codigo ?? '';
  }

  alternarEstado(persona: Persona): void {
    this.error = null;
    this.admin.cambiarEstadoPersona(persona.id, persona.activo !== 'S').subscribe({
      next: actualizada => {
        this.personas = this.personas.map(p => (p.id === actualizada.id ? actualizada : p));
        this.filtrar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  /**
   * Freno de emergencia para un QR comprometido: revoca sin reemitir.
   *
   * <p>No hay acción de EMITIR: el código nace solo en cuanto la persona tiene
   * foto, y ella lo ve en su propia pantalla. Emitirlo a mano era un paso que
   * nadie tenía que dar.</p>
   */
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

  /** Persona que se va a habilitar de nuevo. Null = hoja cerrada. */
  habilitando: Persona | null = null;

  alternarBloqueo(persona: Persona): void {
    if (this.bloqueada(persona)) {
      this.habilitando = persona;
    } else {
      this.bloqueando = persona;
    }
  }

  get mensajeHabilitar(): string {
    const persona = this.habilitando;
    if (!persona) {
      return '';
    }
    return `${persona.nombreCompleto} está deshabilitada por: `
      + `${persona.motivoBloqueo || 'sin motivo registrado'}. `
      + 'Al habilitarla, vuelve a poder ingresar si su hogar la tiene activa.';
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

  confirmarHabilitar(): void {
    const persona = this.habilitando;
    if (!persona) {
      return;
    }

    this.error = null;
    this.admin.desbloquear('personas', persona.id).subscribe({
      next: () => {
        this.habilitando = null;
        this.cargar(this.texto);
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos habilitarla.';
        this.habilitando = null;
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
    claveNueva: ['', [Validators.required, validadorPin()]],
    confirmacion: ['', [Validators.required]]
  });

  /** Sub-hoja para escribir el PIN. Se abre encima de la de acceso. */
  cambiandoClave = false;

  /** Recalcula la lista visible. Se llama tras cargar y tras cada cambio. */
  filtrar(): void {
    this.visibles = this.filtro.aplicar(this.personas);
  }

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

  /**
   * Atajo directo al PIN desde la fila.
   *
   * <p>Abre las dos hojas de una vez: la de acceso queda debajo, así que
   * cerrar la del PIN deja al administrador donde puede seguir administrando
   * la cuenta en vez de devolverlo a la tabla.</p>
   */
  abrirCambioDePin(persona: Persona): void {
    this.abrirAcceso(persona);
    this.cambiandoClave = true;
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
        this.avisoAcceso = `Ya puede entrar con el PIN ${this.claveInicial}.`;
        this.refrescarAcceso();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos crear la cuenta.';
      }
    });
  }

  // La hoja quedó siendo SOLO el cambio de PIN. Lo que vivía aquí —cambiar el
  // rol, activar o desactivar la cuenta, bloquearla— repetía en un segundo
  // sitio lo que los iconos de la fila ya hacen sobre la persona, y obligaba a
  // preguntarse cuál de los dos había que tocar. Los endpoints del API siguen
  // existiendo por si vuelven a hacer falta.

  // Sin "volver a 0000": era una segunda forma de hacer lo mismo. Si hay que
  // dejar la cuenta con el PIN inicial, se escribe en los mismos dos campos.
  // Un módulo con dos caminos para un solo resultado obliga a elegir entre
  // ellos antes de poder actuar.

  /**
   * Qué le pasa al PIN que se está asignando.
   *
   * <p>Elegir cuál regla se rompió es lógica de negocio y no va en la
   * plantilla. Además es la misma fuente que decide si se pinta el mensaje, así
   * que el texto no puede desincronizarse del estado del campo.</p>
   */
  get errorPinAsignado(): string | null {
    const campo = this.formularioClave.controls.claveNueva;
    if (!campo.invalid || !campo.touched) {
      return null;
    }
    return campo.hasError('pinTrivial')
      ? 'Ese PIN es muy fácil de adivinar. Evita repetidos y seguidos.'
      : 'El PIN son 4 números';
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
          this.avisoAcceso = 'PIN asignado. Deberá cambiarlo en su próximo ingreso.';
          this.refrescarAcceso();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos asignar el PIN.';
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
      this.filtrar();
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

  /**
   * Los roles que ESTE administrador puede repartir. Espejo de
   * `Autoridad.rolesAsignablesPor` en el backend, que es quien decide de
   * verdad: acá solo se evita ofrecer una opción que va a responder 403
   * después de llenar todo el formulario.
   *
   * Nadie nombra un par suyo: un administrador que puede crear otro
   * administrador convierte una cuenta comprometida en varias, y quitarle el
   * acceso al primero ya no cierra nada.
   */
  private asignables(roles: Parametro[]): Parametro[] {
    if (this.auth.tieneRol('SUPER_ADMIN')) {
      return roles;
    }
    return roles.filter(r => r.codigo !== 'ADMIN' && r.codigo !== 'SUPER_ADMIN');
  }

  // ── Carga masiva ─────────────────────────────────────────────────────────

  importando = false;
  resultado: ImportacionPersonas | null = null;

  /**
   * La plantilla la genera el SERVIDOR con las mismas columnas que lee el
   * importador: una escrita a mano acá se separa del lector al primer cambio.
   */
  descargarPlantilla(): void {
    this.error = null;
    this.admin.plantillaPersonas().subscribe({
      next: libro => this.descargar(libro, 'plantilla-personas.xlsx'),
      error: () => (this.error = 'No pudimos generar la plantilla.')
    });
  }

  archivoElegido(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    // Se limpia el input para que elegir el MISMO archivo dos veces seguidas
    // vuelva a disparar el evento; si no, corregir el Excel y reintentar sin
    // cambiarle el nombre no haría nada.
    entrada.value = '';
    if (!archivo) {
      return;
    }

    this.importando = true;
    this.error = null;
    this.resultado = null;

    this.admin.importarPersonas(archivo).subscribe({
      next: resultado => {
        this.resultado = resultado;
        this.importando = false;
        // Se recarga siempre, aunque haya rechazos: las filas buenas ya entraron.
        this.cargar(this.texto);
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cargar el archivo.';
        this.importando = false;
      }
    });
  }

  private descargar(contenido: Blob, nombre: string): void {
    const url = URL.createObjectURL(contenido);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    // Sin esto el blob se queda en memoria hasta que se cierre la pestaña.
    URL.revokeObjectURL(url);
  }

  /** El parentesco solo aplica si la persona vive en una casa. */
  get pideParentesco(): boolean {
    return !!this.formulario.controls.casaId.value;
  }

  /**
   * En una casa vacía, el primero que entra ES el titular.
   *
   * <p>El backend lo exige —una casa sin titular deja a todos sus residentes
   * viendo un botón que no funciona—, así que el formulario lo deja puesto en
   * vez de esperar a rechazar el guardado. El selector se bloquea para que no
   * quede la duda de si se podía elegir otra cosa.</p>
   */
  get casaVacia(): boolean {
    const id = this.formulario.controls.casaId.value;
    return !!id && (this.casas.find(c => c.id === id)?.residentes ?? 0) === 0;
  }

  private sincronizarParentescoConLaCasa(casaId: number | null): void {
    if (!casaId) {
      return;
    }
    if (this.casaVacia) {
      this.formulario.controls.parentesco.setValue('TITULAR');
    }
  }

  /**
   * Un guardia no vive en el conjunto: trabaja en él. Ofrecerle una casa lo
   * metería en un núcleo familiar, con los vehículos y los invitados de esa
   * casa detrás.
   *
   * <p>En el alta manda el rol que se está eligiendo. En la edición manda el
   * que la persona ya tiene, porque ahí el selector de cuenta no se muestra —
   * la cuenta se administra desde la columna Acceso.</p>
   */
  get pideCasa(): boolean {
    const rol = this.editando
      ? this.editando.rol
      : this.formulario.controls.rolUsuario.value;
    return rol !== 'GUARDIA';
  }

  /**
   * Al pasar a guardia se limpia la casa elegida antes.
   *
   * <p>Sin esto el campo desaparece de la pantalla pero su valor sigue en el
   * formulario y viaja igual al backend: el administrador ve un guardia y
   * guarda a un residente.</p>
   */
  private sincronizarCasaConElRol(rol: string | null): void {
    if (rol === 'GUARDIA') {
      this.formulario.patchValue({ casaId: null, parentesco: '' });
    }
  }

  /**
   * El correo es obligatorio SOLO cuando el alta incluye cuenta: es por donde
   * esa persona recuperará su PIN. La mayoría de personas del conjunto
   * no tienen cuenta —los niños, quienes solo pasan por la portería— y a esas
   * pedirles correo sería inventar un dato que nadie va a usar.
   */
  get exigeCorreo(): boolean {
    return !this.editando && !!this.formulario.controls.rolUsuario.value;
  }

  /**
   * El validador se enciende y apaga con el rol. Dejarlo fijo bloquearía el
   * alta de todas las personas sin cuenta; no ponerlo dejaría que el formulario
   * se enviara para que el backend lo rechace, que es peor: el error llega
   * después de perder lo digitado.
   */
  private sincronizarValidadorDeCorreo(): void {
    const correo = this.formulario.controls.email;
    correo.setValidators(this.exigeCorreo
      ? [Validators.required, Validators.email]
      : [Validators.email]);
    correo.updateValueAndValidity({ emitEvent: false });
  }
}
