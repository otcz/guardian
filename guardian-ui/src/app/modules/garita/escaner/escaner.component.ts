import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Html5Qrcode } from 'html5-qrcode';

import { AccesoService } from '../../../core/services/acceso.service';
import { AdminService } from '../../../core/services/admin.service';
import { FotoService } from '../../../core/services/foto.service';
import { Parametro } from '../../../core/models/admin.model';
import {
  AccesoEvento,
  FichaVerificacion,
  Modo,
  Presencia,
  Sentido,
  VehiculoResumen
} from '../../../core/models/acceso.model';

type Etapa = 'escaneando' | 'verificando' | 'ficha' | 'registrado';

export type ModoEntrada = 'CAMARA' | 'DOCUMENTO' | 'HUELLA';

interface OpcionModo {
  valor: ModoEntrada;
  etiqueta: string;
  icono: string;
  disponible: boolean;
}

/**
 * Prefijo de los códigos de GUARDIAN: GRD1 para credenciales, GRDI para
 * invitaciones. Sirve para que UN solo campo atienda al lector USB —que
 * "teclea" lo que escanea— sin preguntarle al guardia qué acaba de leer.
 */
const PREFIJO_CODIGO = 'GRD';

const ID_LECTOR = 'gd-lector';

/** Dónde recuerda esta tablet si el guardia dejó el paso automático activo. */
const CLAVE_AUTO = 'guardian.porteria.auto';

/**
 * Segundos que espera el paso automático antes de registrar solo.
 *
 * <p>Diez: alcanzan para mirar la cara, compararla con la foto y tocar una
 * placa si hace falta. Menos, y el guardia registraría a alguien mientras
 * todavía lo está mirando.</p>
 */
const SEGUNDOS_AUTO = 10;

/**
 * Segundos que una credencial recién procesada queda "enfriada": una nueva
 * lectura de la MISMA credencial en ese tiempo se ignora en vez de abrir una
 * ficha nueva.
 *
 * <p>Sin esto, dejar el QR o la cédula frente al lector después de registrar
 * —con paso automático, o simplemente porque el residente no guardó el
 * teléfono de una— hace que la cámara la vuelva a leer, abra una ficha nueva
 * y (con paso automático) la registre otra vez sola. Como la presencia ya
 * cambió, sale en el sentido contrario: entra, sale, entra, sale, cada pocos
 * segundos, para siempre. Es la misma persona que no se movió, no alguien
 * nuevo.</p>
 */
const SEGUNDOS_ENFRIAMIENTO = 20;

/**
 * Operación de la portería.
 *
 * <p>Todo el diseño obedece a que esto se usa de pie, con una tablet, de noche
 * y con gente esperando: la foto manda, el veredicto se lee de un vistazo, y
 * después del escaneo solo queda un toque (a pie o placa).</p>
 */
@Component({
  selector: 'gd-escaner',
  templateUrl: './escaner.component.html',
  styleUrl: './escaner.component.scss',
  standalone: false
})
export class EscanerComponent implements OnInit, OnDestroy {

  etapa: Etapa = 'escaneando';
  ficha: FichaVerificacion | null = null;
  error: string | null = null;

  /** Contadores del encabezado. Se refrescan tras cada registro. */
  presencia: Presencia | null = null;

  /**
   * Cómo se está identificando a quien llega. Son los tres lectores que hay en
   * una portería de verdad, y NO tres formas de hacer lo mismo: la cámara lee
   * el QR firmado; el documento cubre el lector de código de barras y la
   * cédula tecleada; la huella espera hardware.
   */
  modoEntrada: ModoEntrada = 'CAMARA';

  /** Lo que se escanea o se teclea en el modo Documento. */
  entradaManual = '';

  readonly modos: OpcionModo[] = [
    { valor: 'CAMARA', etiqueta: 'Cámara', icono: 'pi-camera', disponible: true },
    { valor: 'DOCUMENTO', etiqueta: 'Documento', icono: 'pi-id-card', disponible: true },
    // Sin lector conectado no se puede prometer: se ve, se entiende que existe,
    // y dice por qué no se puede usar todavía.
    { valor: 'HUELLA', etiqueta: 'Huella', icono: 'pi-stop-circle', disponible: false }
  ];

  /**
   * Sentido elegido por el guardia cuando corrige el inferido (CONTEXT.md §4).
   * Null = aceptar la sugerencia del sistema, que es el caso normal.
   */
  sentidoCorregido: Sentido | null = null;

  /** Evita el doble toque en "A pie"/placa: dos POST concurrentes duplicarían. */
  registrando = false;

  /**
   * Lo que quedó escrito en la bitácora. Es la fuente del veredicto final:
   * el registro puede negar aunque la verificación de hace tres segundos
   * hubiera dicho que sí.
   */
  eventoRegistrado: AccesoEvento | null = null;

  /** Catálogo de motivos, para no mostrarle un código al guardia. */
  motivos: Parametro[] = [];

  get permitido(): boolean {
    return this.eventoRegistrado?.resultado !== 'DENEGADO';
  }

  // ── Paso automático ──────────────────────────────────────────────────────
  //
  // En hora pico la fila no espera a que el guardia toque "A pie" cuarenta
  // veces. Con esto activo, una ficha en verde se registra sola a los diez
  // segundos —a pie, que es lo más común y lo único que no le atribuye un
  // carro a nadie— y el guardia solo interviene cuando NO es eso.
  //
  // Se queda apagado por defecto y es decisión de cada tablet: la puerta
  // peatonal de un conjunto pequeño no lo necesita.

  /** Si esta tablet registra sola cuando el guardia no toca nada. */
  autoRegistro = false;

  /** Lo que falta para que se registre solo. 0 = no hay cuenta corriendo. */
  segundosRestantes = 0;

  /** Para que la pantalla diga el número real y no uno escrito a mano. */
  readonly SEGUNDOS_AUTO = SEGUNDOS_AUTO;

  private lector: Html5Qrcode | null = null;
  private procesando = false;
  private cuenta: ReturnType<typeof setInterval> | null = null;

  /**
   * Credencial leída mientras otra ficha seguía en pantalla.
   *
   * <p>Es el caso que hace útil el paso automático: si ya llegó el siguiente,
   * el anterior no tiene por qué esperar sus diez segundos. Se registra con el
   * default de una vez y la fila sigue moviéndose.</p>
   */
  private colaLectura: string | null = null;

  /**
   * La última credencial registrada, y hasta cuándo sigue "enfriada". Null =
   * nadie se acaba de procesar, o ya pasó el enfriamiento.
   */
  private ultimaProcesada: { payload: string | null; documento: string | null; hasta: number }
    | null = null;

  constructor(
    private readonly acceso: AccesoService,
    private readonly admin: AdminService,
    private readonly fotoService: FotoService
  ) {}

  ngOnInit(): void {
    this.autoRegistro = localStorage.getItem(CLAVE_AUTO) === 'S';
    this.cargarPresencia();
    this.iniciarCamara();
    // El motivo se muestra con el texto del catálogo, no con el código:
    // "VEHICULO_INACTIVO" no es algo que se le pueda leer a nadie de noche.
    this.admin.parametros('MOTIVO_DENEGACION').subscribe(m => (this.motivos = m));
  }

  private cargarPresencia(): void {
    this.acceso.presencia().subscribe({
      next: presencia => (this.presencia = presencia),
      error: () => undefined
    });
  }

  ngOnDestroy(): void {
    this.detenerCamara();
    this.detenerCuenta();
  }

  // ── Cámara ───────────────────────────────────────────────────────────────

  private iniciarCamara(): void {
    this.lector = new Html5Qrcode(ID_LECTOR);

    this.lector
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 240, height: 240 } },
        texto => this.alLeer(texto),
        () => {
          // El callback de "no encontré nada en este cuadro" se dispara varias
          // veces por segundo. Ignorarlo es lo correcto: no es un error.
        }
      )
      .catch(() => {
        // Caer al documento y no dejar la pantalla muerta: la fila sigue ahí.
        this.modoEntrada = 'DOCUMENTO';
        this.error = 'No pudimos abrir la cámara. Usa el documento.';
      });
  }

  private detenerCamara(): void {
    if (!this.lector) {
      return;
    }
    this.lector.stop().catch(() => undefined);
    this.lector = null;
  }

  private alLeer(payload: string): void {
    // La misma credencial que se acaba de registrar, todavía frente a la
    // cámara: no es alguien nuevo. Se ignora entera y sin abrir ficha —
    // rompe el ciclo entra-sale-entra-sale del paso automático.
    if (this.enEnfriamiento(payload)) {
      return;
    }
    // Con paso automático la cámara NO se apaga sobre la ficha: es lo que
    // permite que el siguiente en la fila empuje al anterior. Fuera de ese
    // modo el comportamiento es el de siempre — se apaga y se decide con
    // calma.
    if (this.etapa === 'ficha') {
      this.alLlegarOtro(payload);
      return;
    }
    if (this.etapa !== 'escaneando') {
      return;
    }
    // La cámara sigue leyendo el mismo QR mientras esté al frente. Sin esta
    // guarda se dispararían decenas de peticiones por un solo escaneo.
    if (this.procesando) {
      return;
    }
    this.procesando = true;
    if (!this.autoRegistro) {
      this.detenerCamara();
    }
    this.verificar(payload);
  }

  /**
   * Alguien nuevo se paró frente al lector con una ficha todavía en pantalla.
   *
   * <p>Se registra la anterior con el default y se pasa a la nueva. Sin esto,
   * el que llega tendría que esperar a que se agote la cuenta del que ya se
   * fue.</p>
   */
  private alLlegarOtro(lectura: string): void {
    // La misma credencial delante de la cámara no es alguien nuevo: es el QR
    // que sigue ahí. Y sin ficha permitida no hay nada que registrar solo —
    // un rechazo se resuelve mirándolo, no dejando que lo empujen.
    const esElMismo = lectura === this.ficha?.payload || lectura === this.ficha?.documento;
    if (!this.autoRegistro || this.registrando || esElMismo || !this.ficha?.permitido) {
      return;
    }
    this.detenerCuenta();
    this.colaLectura = lectura;
    this.registrar('PEATON');
  }

  // ── Flujo ────────────────────────────────────────────────────────────────

  /**
   * Un solo campo para el lector de código de barras Y para la cédula tecleada.
   *
   * <p>Un lector USB "teclea" lo que escanea en el campo que tenga el foco, así
   * que aquí puede caer el código de barras de una cédula o el QR de GUARDIAN.
   * El prefijo distingue los dos mundos sin preguntarle nada al guardia, que es
   * quien menos tiempo tiene para responder preguntas.</p>
   */
  verificarEntradaManual(): void {
    const texto = this.entradaManual.trim();
    if (!texto) {
      return;
    }
    // Mismo freno que en la cámara: el lector de barras y la cédula tecleada
    // pueden repetir la misma lectura segundos después de procesarla. No se
    // limpia el campo: si esto dispara es porque ya se proceso, y borrarlo
    // ahora seria justo el problema que se corrigio mas abajo — que el
    // guardia pierda lo que escribio sin haber terminado nada nuevo.
    if (this.enEnfriamiento(texto)) {
      return;
    }

    // El lector de barras teclea igual sobre una ficha abierta que sobre la
    // pantalla de escaneo. Si ya hay alguien en pantalla, el que acaba de
    // llegar lo empuja — exactamente como con la cámara.
    if (this.etapa === 'ficha') {
      this.entradaManual = '';
      this.alLlegarOtro(texto);
      return;
    }

    this.procesando = true;
    this.procesarLectura(texto);
  }

  /**
   * Una lectura, venga de donde venga. El prefijo distingue el QR de GUARDIAN
   * de una cédula sin preguntarle nada al guardia, que es quien menos tiempo
   * tiene para responder preguntas.
   */
  private procesarLectura(texto: string): void {
    if (texto.toUpperCase().startsWith(PREFIJO_CODIGO)) {
      this.verificar(texto);
    } else {
      this.verificarDocumento(texto);
    }
  }

  private verificarDocumento(documento: string): void {
    this.etapa = 'verificando';
    this.error = null;

    this.acceso.verificarPorDocumento(documento).subscribe({
      next: ficha => {
        this.ficha = ficha;
        this.etapa = 'ficha';
        this.arrancarCuenta();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos verificar el documento.';
        this.etapa = 'escaneando';
        this.procesando = false;
        // A propósito NO se limpia entradaManual: si el guardia se equivocó en
        // un dígito, tiene que poder corregirlo, no volver a escribir el
        // número entero. reiniciar() sí lo borra, y por eso no se usa acá.
      }
    });
  }

  elegirModo(modo: ModoEntrada): void {
    if (modo === this.modoEntrada || !this.modos.find(m => m.valor === modo)?.disponible) {
      return;
    }
    this.modoEntrada = modo;
    this.error = null;
    this.entradaManual = '';

    if (modo === 'CAMARA') {
      this.iniciarCamara();
    } else {
      this.detenerCamara();
    }
  }

  private verificar(payload: string): void {
    this.etapa = 'verificando';
    this.error = null;

    this.acceso.verificar(payload).subscribe({
      next: ficha => {
        this.ficha = { ...ficha, payload: ficha.payload ?? payload };
        this.etapa = 'ficha';
        this.arrancarCuenta();
      },
      error: (fallo: HttpErrorResponse) => {
        this.error = fallo.error?.mensaje ?? 'No pudimos verificar el código.';
        this.reiniciar();
      }
    });
  }

  registrar(modo: Modo, vehiculo?: VehiculoResumen): void {
    if (!this.ficha?.payload || this.registrando) {
      return;
    }
    // Tocar cualquier opción es intervenir: la cuenta se corta aunque el toque
    // sea justo el mismo default que iba a aplicarse.
    this.detenerCuenta();
    this.registrando = true;
    this.error = null;

    const sentido = this.sentidoCorregido ?? this.ficha.sentidoSugerido;

    this.acceso
      .registrar({
        payload: this.ficha.payload,
        modo,
        vehiculoId: vehiculo?.id ?? null,
        sentido,
        corregirSentido: this.sentidoCorregido !== null
      })
      .subscribe({
        next: evento => {
          // Se marca ANTES de tocar this.ficha: reiniciar() y limpiarFicha()
          // lo van a poner en null unas líneas más abajo.
          this.ultimaProcesada = {
            payload: this.ficha?.payload ?? null,
            documento: this.ficha?.documento ?? null,
            hasta: Date.now() + SEGUNDOS_ENFRIAMIENTO * 1000
          };

          // Este es el único punto que borra lo que el guardia tecleó: se
          // completó un registro de verdad, así que el campo queda listo para
          // la SIGUIENTE persona. limpiarFicha() ya NO lo toca — cancelar una
          // ficha o que el documento no aparezca no puede costarle al guardia
          // reescribir el número entero por un solo dígito equivocado.
          this.entradaManual = '';

          // El registro puede volver DENEGADO con 200: entre el escaneo y el
          // toque pasan segundos, y en esos segundos el administrador pudo
          // deshabilitar el carro o revocar la credencial. Antes se pintaba
          // verde igual — el guardia dejaba pasar justo el caso que motivó el
          // bloqueo. Ahora el veredicto sale del evento, no del código HTTP.
          this.eventoRegistrado = evento;
          this.cargarPresencia();

          // Ya había alguien esperando: se salta la confirmación y se le
          // atiende de una vez. La fila no espera dos segundos por cortesía.
          if (this.colaLectura) {
            const siguiente = this.colaLectura;
            this.colaLectura = null;
            this.limpiarFicha();
            // Por procesarLectura y NO por verificar(): en la cola puede haber
            // caído un QR o una cédula, y mandar una cédula al endpoint del QR
            // dejaba la pantalla en blanco con el siguiente ya esperando.
            this.procesarLectura(siguiente);
            return;
          }

          this.etapa = 'registrado';
          // Vuelve solo a escanear: en hora pico nadie va a tocar "siguiente".
          // El rojo se queda más tiempo: hay que leer POR QUÉ no entra.
          setTimeout(() => this.reiniciar(), this.permitido ? 2000 : 5000);
        },
        error: (fallo: HttpErrorResponse) => {
          this.registrando = false;
          this.colaLectura = null;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar el ingreso.';
        }
      });
  }

  // ── Paso automático ──────────────────────────────────────────────────────

  /** El check de la pantalla. Se recuerda por tablet, no por usuario. */
  alternarAuto(): void {
    this.autoRegistro = !this.autoRegistro;
    localStorage.setItem(CLAVE_AUTO, this.autoRegistro ? 'S' : 'N');

    if (!this.autoRegistro) {
      this.detenerCuenta();
    }
  }

  /**
   * Corta la cuenta sin registrar. Es el freno del guardia: la ficha se queda
   * quieta en pantalla y él decide con calma.
   */
  cancelarAuto(): void {
    this.detenerCuenta();
  }

  get cuentaCorriendo(): boolean {
    return this.segundosRestantes > 0;
  }

  /**
   * Arranca la cuenta solo sobre una ficha en verde.
   *
   * <p>Un rechazo NO se registra solo: es justo el caso que el guardia tiene
   * que leer, y encima el sentido de una negación no la resuelve un
   * temporizador.</p>
   */
  private arrancarCuenta(): void {
    this.detenerCuenta();
    if (!this.autoRegistro || !this.ficha?.permitido) {
      return;
    }

    this.segundosRestantes = SEGUNDOS_AUTO;
    this.cuenta = setInterval(() => {
      this.segundosRestantes--;
      if (this.segundosRestantes <= 0) {
        this.detenerCuenta();
        // A pie: es lo más común, y es la única opción que no le atribuye un
        // vehículo a alguien que quizá venía caminando.
        this.registrar('PEATON');
      }
    }, 1000);
  }

  private detenerCuenta(): void {
    if (this.cuenta) {
      clearInterval(this.cuenta);
      this.cuenta = null;
    }
    this.segundosRestantes = 0;
  }

  /** Un toque alterna; el sistema ya acertó casi siempre, esto es la excepción. */
  alternarSentido(): void {
    // Corregir el sentido es intervenir: quien lo toca está decidiendo, y que
    // el temporizador le registre encima sería el peor momento posible.
    this.detenerCuenta();
    const sugerido = this.ficha?.sentidoSugerido ?? 'E';
    if (this.sentidoCorregido === null) {
      this.sentidoCorregido = sugerido === 'E' ? 'S' : 'E';
    } else {
      this.sentidoCorregido = null;
    }
  }

  reiniciar(): void {
    this.limpiarFicha();
    this.eventoRegistrado = null;
    this.etapa = 'escaneando';

    // Vuelve al modo que el guardia venía usando: si eligió documento porque el
    // lector de barras está en esa puerta, no tiene por qué reelegirlo cada vez.
    // Con paso automático la cámara nunca se apagó, así que no hay que
    // reabrirla — hacerlo dos veces deja dos streams peleando por el mismo
    // dispositivo.
    if (this.modoEntrada === 'CAMARA' && !this.lector) {
      this.iniciarCamara();
    }
  }

  /**
   * Si esta lectura es la misma credencial que se acaba de registrar y el
   * enfriamiento todavía no pasó. Compara contra payload Y documento porque la
   * misma persona puede volver a aparecer por cualquiera de los dos lectores.
   */
  private enEnfriamiento(lectura: string): boolean {
    const previa = this.ultimaProcesada;
    if (!previa || Date.now() >= previa.hasta) {
      return false;
    }
    return lectura === previa.payload || lectura === previa.documento;
  }

  /**
   * Lo que se borra entre una persona y la siguiente, sin tocar la etapa.
   *
   * <p>NO toca {@link entradaManual} a propósito: esto se llama tanto al
   * cerrar una ficha ya registrada como al cancelar una que no se registró
   * —incluida la de "documento no encontrado"—, y solo en el primer caso el
   * campo debe quedar vacío. Ese caso lo limpia {@link registrar} en su
   * propio punto. Si esta función lo borrara siempre, cancelar por un
   * documento mal tecleado obligaría a escribirlo entero otra vez.</p>
   */
  private limpiarFicha(): void {
    this.detenerCuenta();
    this.ficha = null;
    this.procesando = false;
    this.registrando = false;
    this.sentidoCorregido = null;
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  /**
   * Alguien DESHABILITADO que está adentro: el servidor le permite salir
   * —retener a una persona dentro del conjunto no protege a nadie— pero no
   * volver a entrar, y lo avisa mandando un mensaje con la ficha en verde.
   *
   * <p>Es el único caso donde `permitido` es true y aun así hay algo que
   * decir. Sin distinguirlo, la pantalla pintaba el mismo verde que a un
   * residente al día y el guardia no tenía forma de saberlo.</p>
   */
  get soloSalida(): boolean {
    return !!this.ficha?.permitido && !!this.ficha?.mensaje;
  }

  get textoVeredicto(): string {
    if (!this.ficha?.permitido) {
      return 'NO PUEDE PASAR';
    }
    return this.soloSalida ? 'SOLO PUEDE SALIR' : 'PUEDE PASAR';
  }

  get iconoVeredicto(): string {
    if (!this.ficha?.permitido) {
      return 'pi-times-circle';
    }
    return this.soloSalida ? 'pi-exclamation-triangle' : 'pi-check-circle';
  }

  get textoSentido(): string {
    const sentido: Sentido | null | undefined =
      this.sentidoCorregido ?? this.ficha?.sentidoSugerido;
    return sentido === 'S' ? 'SALIDA' : 'ENTRADA';
  }

  /** La flecha del sentido: entra hacia adentro, sale hacia afuera. */
  get iconoSentido(): string {
    return this.textoSentido === 'SALIDA' ? 'pi-sign-out' : 'pi-sign-in';
  }

  get textoSentidoOpuesto(): string {
    return this.textoSentido === 'SALIDA' ? 'ENTRADA' : 'SALIDA';
  }

  get tieneVehiculos(): boolean {
    return (this.ficha?.vehiculos?.length ?? 0) > 0;
  }

  get fotoFicha(): string | null {
    return this.fotoService.urlAbsoluta(this.ficha?.fotoUrl ?? null);
  }
}
