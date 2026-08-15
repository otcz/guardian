import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ResidenteService } from '../../../core/services/residente.service';
import { AdminService } from '../../../core/services/admin.service';
import {
  CasaDisponible,
  CodigoHogar,
  Familiar,
  Parametro,
  SolicitudCasa,
  SolicitudVehiculo,
  Vehiculo
} from '../../../core/models/admin.model';

/**
 * Autogestión del residente: su núcleo familiar (esposa, hijos, invitados) y
 * los vehículos de su casa. Aquí no existe eliminar — solo activar y desactivar;
 * la eliminación es exclusiva del administrador.
 */
@Component({
  selector: 'gd-mi-hogar',
  templateUrl: './mi-hogar.component.html',
  styleUrl: './mi-hogar.component.scss',
  standalone: false
})
export class MiHogarComponent implements OnInit {

  private readonly fb = inject(FormBuilder);

  familia: Familiar[] = [];

  /**
   * Solo el titular arma el núcleo. Se resuelve ACÁ y no esperando el 403:
   * dejar que cualquiera abra el formulario, lo llene entero y recién ahí
   * recibir "no puedes" es la peor forma de comunicar una regla.
   */
  get esTitular(): boolean {
    return this.familia.some(f => f.esUsuarioActual && f.parentesco === 'TITULAR');
  }

  /** A quién hay que pedírselo. Sin el nombre, el aviso deja sin salida. */
  get nombreDelTitular(): string | null {
    return this.familia.find(f => f.parentesco === 'TITULAR')?.nombreCompleto ?? null;
  }

  /**
   * Solo la propia foto se puede cambiar desde acá. El backend firma
   * "mi-foto" contra la persona del token — no hay forma de ponerle la cara a
   * otro, ni siquiera siendo el titular. Cambiar la foto de un familiar sigue
   * siendo cosa de la administración.
   */
  get miFotoActual(): string | null {
    return this.familia.find(f => f.esUsuarioActual)?.fotoUrl ?? null;
  }

  cambiandoFoto = false;
  guardandoFoto = false;

  vehiculos: Vehiculo[] = [];

  /**
   * Lo que la casa pidió y la administración todavía no ha respondido, más los
   * rechazos que el titular no ha descartado.
   *
   * <p>Se muestran junto a los vehículos y no en otra pantalla: la pregunta del
   * residente es "¿mi carro ya entra?", y la respuesta tiene que estar donde
   * están los carros.</p>
   */
  solicitudesVehiculo: SolicitudVehiculo[] = [];

  parentescos: Parametro[] = [];
  tiposVehiculo: Parametro[] = [];
  marcasVehiculo: Parametro[] = [];
  coloresVehiculo: Parametro[] = [];
  tiposDocumento: Parametro[] = [];

  cargando = true;
  sinCasa = false;
  error: string | null = null;

  // ── Todavía sin casa ─────────────────────────────────────────────────────
  //
  // Antes esto era un callejón sin salida: "tu usuario no tiene una casa
  // asignada" y nada más que hacer. Ahora elige la suya y la administración
  // aprueba — pedir no asigna nada.

  casasDisponibles: CasaDisponible[] = [];
  solicitud: SolicitudCasa | null = null;
  casaElegida: number | null = null;
  parentescoElegido = '';
  enviandoSolicitud = false;

  /** true mientras espera respuesta: no puede pedir otra encima. */
  get solicitudPendiente(): boolean {
    return this.solicitud?.estado === 'PENDIENTE';
  }

  get solicitudRechazada(): boolean {
    return this.solicitud?.estado === 'RECHAZADA';
  }

  /**
   * En una casa sin titular el primero que entra TIENE que serlo, o esa
   * familia nace sin quien la administre. El selector se ajusta solo en vez de
   * dejar elegir algo que el backend va a rechazar.
   */
  get soloPuedeSerTitular(): boolean {
    const casa = this.casasDisponibles.find(c => c.id === this.casaElegida);
    return !!casa && !casa.tieneTitular;
  }

  private cargarEstadoSinCasa(): void {
    this.residente.casasDisponibles().subscribe({
      next: casas => (this.casasDisponibles = casas),
      error: () => (this.error = 'No pudimos cargar las casas del conjunto.')
    });
    this.residente.miSolicitud().subscribe({
      next: solicitud => (this.solicitud = solicitud),
      error: () => undefined
    });
  }

  alElegirCasa(): void {
    // El parentesco se recalcula al cambiar de casa: si venía en HIJO y elige
    // una casa vacía, quedaría pidiendo algo imposible.
    this.parentescoElegido = this.soloPuedeSerTitular ? 'TITULAR' : '';
  }

  solicitarCasa(): void {
    if (!this.casaElegida || !this.parentescoElegido || this.enviandoSolicitud) {
      return;
    }
    this.enviandoSolicitud = true;
    this.error = null;

    this.residente.solicitarCasa(this.casaElegida, this.parentescoElegido).subscribe({
      next: solicitud => {
        this.solicitud = solicitud;
        this.enviandoSolicitud = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos enviar la solicitud.';
        this.enviandoSolicitud = false;
      }
    });
  }

  /** Volver a elegir después de un rechazo. */
  elegirDeNuevo(): void {
    this.solicitud = null;
    this.casaElegida = null;
    this.parentescoElegido = '';
  }

  mostrarAltaFamiliar = false;
  mostrarAltaVehiculo = false;
  guardando = false;

  /**
   * Acción destructiva esperando confirmación. Desactivar a un familiar le
   * quita el ingreso al conjunto: no puede pasar por un toque accidental,
   * y hasta ahora no preguntaba nada.
   */
  aConfirmar: { titulo: string; detalle: string; etiqueta: string; accion: () => void } | null = null;

  readonly formularioFamiliar = this.fb.nonNullable.group({
    tipoDocumento: ['CC', [Validators.required]],
    documento: ['', [Validators.required]],
    nombres: ['', [Validators.required]],
    apellidos: ['', [Validators.required]],
    fechaNacimiento: [''],
    fotoUrl: [null as string | null],
    telefono: [''],
    // Opcional a propósito: con correo el familiar recibe cuenta y entra a la
    // aplicación; sin él queda registrado solo para la portería, que es el caso
    // del niño que todavía no tiene correo propio.
    email: ['', [Validators.email]],
    parentesco: ['', [Validators.required]]
  });

  readonly formularioVehiculo = this.fb.nonNullable.group({
    placa: ['', [Validators.required]],
    tipo: ['', [Validators.required]],
    marca: [''],
    color: [''],
    // Sin Validators: al carro lo identifica su placa, no su foto. Exigirla
    // dejaría al titular sin poder pedir el vehículo porque el celular no
    // tiene batería para tomar la foto ahora.
    fotoUrl: [null as string | null]
  });

  constructor(
    private readonly residente: ResidenteService,
    private readonly admin: AdminService
  ) {}

  ngOnInit(): void {
    this.cargar();
    // El catálogo de parentescos excluye TITULAR en el formulario: ese lo
    // asigna la administración.
    this.admin.parametros('PARENTESCO').subscribe(parametros => {
      this.parentescos = parametros.filter(p => p.codigo !== 'TITULAR');
    });
    this.admin.parametros('TIPO_VEHICULO').subscribe(tipos => (this.tiposVehiculo = tipos));
    this.admin.parametros('MARCA_VEHICULO').subscribe(m => (this.marcasVehiculo = m));
    this.admin.parametros('COLOR_VEHICULO').subscribe(c => (this.coloresVehiculo = c));
    this.admin.parametros('TIPO_DOCUMENTO').subscribe(t => (this.tiposDocumento = t));
  }

  // ── Mi foto ──────────────────────────────────────────────────────────────

  abrirCambioFoto(): void {
    this.cambiandoFoto = true;
  }

  guardarFotoPropia(fotoUrl: string | null): void {
    if (!fotoUrl || this.guardandoFoto) {
      return;
    }

    this.guardandoFoto = true;
    this.error = null;

    this.residente.fijarMiFoto(fotoUrl).subscribe({
      next: () => {
        this.guardandoFoto = false;
        this.cambiandoFoto = false;
        // Recarga la fila entera: es la forma mas simple de que la foto
        // nueva se vea sin duplicar aca lo que ya sabe armar la lista.
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardandoFoto = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar tu foto.';
      }
    });
  }

  cargar(): void {
    this.cargando = true;
    // El aviso de error se limpia al reintentar: si sobrevive a una carga
    // buena, queda un banner rojo encima de una lista que si cargo.
    this.error = null;

    this.residente.familia().subscribe({
      next: familia => {
        this.familia = familia;
        this.cargando = false;
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        if (fallo.status === 400) {
          this.sinCasa = true;
          // Recién ahí se piden las casas: al residente con hogar no le hace
          // falta la lista del conjunto y no tiene por qué recibirla.
          this.cargarEstadoSinCasa();
        } else {
          this.error = fallo.error?.mensaje ?? 'No pudimos cargar tu hogar.';
        }
      }
    });

    this.residente.vehiculos().subscribe({
      next: vehiculos => (this.vehiculos = vehiculos),
      error: () => undefined
    });

    this.residente.solicitudesVehiculo().subscribe({
      next: solicitudes => (this.solicitudesVehiculo = solicitudes),
      // Quien todavía no tiene casa recibe un 400 acá: no es un error que
      // mostrar, es que la pantalla de vehículos ni siquiera aplica.
      error: () => (this.solicitudesVehiculo = [])
    });
  }

  // ── Familia ──────────────────────────────────────────────────────────────

  agregarFamiliar(): void {
    if (this.formularioFamiliar.invalid || this.guardando) {
      this.formularioFamiliar.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;
    const datos = this.formularioFamiliar.getRawValue();

    this.residente
      .agregarFamiliar({
        ...datos,
        fechaNacimiento: datos.fechaNacimiento || null,
        telefono: datos.telefono || null,
        // Cadena vacía y null significan lo mismo —no lo declaró— y el backend
        // decide con esto si le crea cuenta o no.
        email: datos.email || null
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarAltaFamiliar = false;
          this.formularioFamiliar.reset({ tipoDocumento: 'CC', fotoUrl: null });
          this.cargar();
        },
        error: (fallo: HttpErrorResponse) => {
          this.guardando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar a la persona.';
        }
      });
  }

  alternarEstadoFamiliar(familiar: Familiar): void {
    // Reactivar no destruye nada: se ejecuta directo. Desactivar sí.
    if (!this.activo(familiar.activo)) {
      this.aplicarEstadoFamiliar(familiar);
      return;
    }
    this.aConfirmar = {
      titulo: familiar.nombreCompleto,
      detalle: 'No podrá entrar al conjunto hasta que lo actives de nuevo.',
      etiqueta: 'Desactivar',
      accion: () => this.aplicarEstadoFamiliar(familiar)
    };
  }

  private aplicarEstadoFamiliar(familiar: Familiar): void {
    this.error = null;
    this.residente
      .cambiarEstadoFamiliar(familiar.personaId, familiar.activo !== 'S')
      .subscribe({
        next: () => this.cargar(),
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
        }
      });
  }

  // ── Vehículos ────────────────────────────────────────────────────────────
  //
  // El titular NO registra el vehículo: lo pide. Una placa registrada es un
  // carro al que la portería le abre sin volver a preguntar, y eso lo autoriza
  // la administración.

  agregarVehiculo(): void {
    if (this.formularioVehiculo.invalid || this.guardando) {
      this.formularioVehiculo.markAllAsTouched();
      return;
    }

    this.guardando = true;
    this.error = null;

    this.residente.solicitarVehiculo(this.formularioVehiculo.getRawValue()).subscribe({
      next: () => {
        this.guardando = false;
        this.mostrarAltaVehiculo = false;
        this.formularioVehiculo.reset({ fotoUrl: null });
        this.cargar();
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos enviar la solicitud.';
      }
    });
  }

  // ── Foto de un vehículo ya autorizado ────────────────────────────────────
  //
  // Aparte de la solicitud: el vehículo nace cuando la administración aprueba,
  // así que los carros autorizados antes de que existiera este campo se
  // quedarían sin foto para siempre si solo se pudiera mandar al pedirlo.

  /** Vehículo al que se le está cambiando la foto. Null = hoja cerrada. */
  vehiculoEnFoto: Vehiculo | null = null;
  guardandoFotoVehiculo = false;

  abrirFotoVehiculo(vehiculo: Vehiculo): void {
    this.vehiculoEnFoto = vehiculo;
  }

  guardarFotoVehiculo(fotoUrl: string | null): void {
    const vehiculo = this.vehiculoEnFoto;
    if (!vehiculo || this.guardandoFotoVehiculo) {
      return;
    }

    this.guardandoFotoVehiculo = true;
    this.error = null;

    this.residente.fijarFotoVehiculo(vehiculo.id, fotoUrl).subscribe({
      next: actualizado => {
        this.guardandoFotoVehiculo = false;
        this.vehiculoEnFoto = null;
        // Se reemplaza la fila y no se recarga todo: la lista de vehículos ya
        // viene completa en la respuesta y una recarga entera parpadea.
        this.vehiculos = this.vehiculos.map(v => (v.id === actualizado.id ? actualizado : v));
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardandoFotoVehiculo = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar la foto del vehículo.';
      }
    });
  }

  /** Quita de la lista un rechazo ya leído. El motivo queda en el histórico. */
  descartarSolicitudVehiculo(solicitud: SolicitudVehiculo): void {
    this.error = null;
    this.residente.descartarSolicitudVehiculo(solicitud.id).subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos quitar la solicitud.';
      }
    });
  }

  alternarEstadoVehiculo(vehiculo: Vehiculo): void {
    if (!this.activo(vehiculo.activo)) {
      this.aplicarEstadoVehiculo(vehiculo);
      return;
    }
    this.aConfirmar = {
      titulo: vehiculo.placa,
      detalle: 'La portería dejará de permitir su ingreso.',
      etiqueta: 'Desactivar',
      accion: () => this.aplicarEstadoVehiculo(vehiculo)
    };
  }

  private aplicarEstadoVehiculo(vehiculo: Vehiculo): void {
    this.error = null;
    this.residente.cambiarEstadoVehiculo(vehiculo.id, vehiculo.activo !== 'S').subscribe({
      next: () => this.cargar(),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos cambiar el estado.';
      }
    });
  }

  confirmar(): void {
    const pendiente = this.aConfirmar;
    this.aConfirmar = null;
    pendiente?.accion();
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  etiquetaParentesco(codigo: string): string {
    return this.parentescos.find(p => p.codigo === codigo)?.valor ?? codigo;
  }

  activo(estado: string): boolean {
    return estado === 'S';
  }

  /**
   * Deshabilitado por la administración: la otra llave. El titular activa y
   * desactiva lo suyo, pero esta no la puede levantar, así que en su lugar ve
   * un candado y no un interruptor — ofrecerle un botón que el backend le va a
   * negar es prometerle algo que no se cumple.
   */
  bloqueado(entidad: { bloqueado: string }): boolean {
    return entidad.bloqueado === 'S';
  }

  // ── Invitar a alguien al hogar ───────────────────────────────────────────
  //
  // La otra vía para armar un núcleo: en vez de que el titular digite a cada
  // familiar, le pasa un enlace y cada uno crea su propia cuenta.

  codigoHogar: CodigoHogar | null = null;
  mostrarInvitacion = false;
  copiado = false;

  abrirInvitacion(): void {
    this.mostrarInvitacion = true;
    this.copiado = false;
    this.residente.codigoHogar().subscribe({
      next: codigo => (this.codigoHogar = codigo),
      error: () => (this.codigoHogar = null)
    });
  }

  generarCodigo(): void {
    if (this.guardando) {
      return;
    }
    this.guardando = true;
    this.error = null;
    this.copiado = false;

    this.residente.generarCodigoHogar().subscribe({
      next: codigo => {
        this.guardando = false;
        this.codigoHogar = codigo;
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos generar el código.';
      }
    });
  }

  revocarCodigo(): void {
    const seguro = window.confirm(
      '¿Anular el enlace? Quien lo tenga dejará de poder registrarse con él.');
    if (!seguro) {
      return;
    }

    this.residente.revocarCodigoHogar().subscribe({
      next: () => (this.codigoHogar = null),
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos anular el código.';
      }
    });
  }

  /** El enlace completo: es lo que se pega en WhatsApp, no el código suelto. */
  get enlaceInvitacion(): string {
    return this.codigoHogar
      ? `${window.location.origin}/unirme/${this.codigoHogar.codigo}`
      : '';
  }

  copiarEnlace(): void {
    navigator.clipboard?.writeText(this.enlaceInvitacion)
      .then(() => (this.copiado = true))
      .catch(() => undefined);
  }

  /**
   * Explicación del candado. Hoja aparte y no la de confirmar: esa ofrece una
   * acción destructiva en rojo, y acá no hay nada que hacer más que leer.
   */
  bloqueoInfo: { titulo: string; detalle: string } | null = null;

  explicarBloqueo(nombre: string, motivo: string | null): void {
    this.bloqueoInfo = {
      titulo: nombre,
      detalle: motivo
        ? `La administración lo deshabilitó: ${motivo}. Comunícate con ella para habilitarlo.`
        : 'La administración lo deshabilitó. Comunícate con ella para habilitarlo.'
    };
  }
}
