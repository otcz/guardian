import {
  AfterViewChecked,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { AccesoService } from '../../../core/services/acceso.service';
import { AdminService } from '../../../core/services/admin.service';
import { FotoService } from '../../../core/services/foto.service';
import { Parametro } from '../../../core/models/admin.model';
import {
  AccesoEvento,
  CandidatoGarita,
  FichaVerificacion,
  Modo,
  Presencia,
  Sentido,
  VehiculoResumen
} from '../../../core/models/acceso.model';

type Etapa = 'escaneando' | 'verificando' | 'ficha' | 'registrado';

/**
 * Prefijo de los códigos de GUARDIAN: GRD1 para credenciales, GRDI para
 * invitaciones. Sirve para que UN solo campo atienda al lector USB —que
 * "teclea" lo que escanea— sin preguntarle al guardia qué acaba de leer.
 */
const PREFIJO_CODIGO = 'GRD';

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
 * <p>Sin esto, pasar dos veces el mismo código por el lector —o que el lector
 * repita la lectura, que es lo normal si el QR se queda al frente— abre una
 * ficha nueva y (con paso automático) la registra otra vez sola. Como la
 * presencia ya cambió, sale en el sentido contrario: entra, sale, entra,
 * sale, cada pocos segundos, para siempre. Es la misma persona que no se
 * movió, no alguien nuevo.</p>
 */
const SEGUNDOS_ENFRIAMIENTO = 20;

/**
 * Operación de la portería.
 *
 * <p>Todo el diseño obedece a que esto se usa de pie, con una tablet, de noche
 * y con gente esperando: la foto manda, el veredicto se lee de un vistazo, y
 * después del escaneo solo queda un toque (a pie o placa).</p>
 *
 * <p>Se identifica por UN solo campo. El lector de QR y el de código de barras
 * son teclados: escriben ahí lo que leen. La cédula tecleada cae en el mismo
 * sitio y el prefijo distingue un código de GUARDIAN de un número de documento.
 * No hay modos que elegir — el guardia no tiene que decirle al sistema qué
 * acaba de pasar por el lector.</p>
 */
@Component({
  selector: 'gd-escaner',
  templateUrl: './escaner.component.html',
  styleUrl: './escaner.component.scss',
  standalone: false
})
export class EscanerComponent implements OnInit, AfterViewChecked, OnDestroy {

  etapa: Etapa = 'escaneando';
  ficha: FichaVerificacion | null = null;
  error: string | null = null;

  /** Contadores del encabezado. Se refrescan tras cada registro. */
  presencia: Presencia | null = null;

  /** Lo que el lector escribe o el guardia teclea. */
  entradaManual = '';

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

  /**
   * El campo donde escribe el lector. Un lector USB teclea en lo que tenga el
   * foco: si el foco se pierde, el escaneo se va a la nada y la portería deja
   * de funcionar sin decir por qué.
   */
  @ViewChild('codigo') private campoCodigo?: ElementRef<HTMLInputElement>;

  /** El mismo papel, pero con una ficha en pantalla y paso automático puesto. */
  @ViewChild('siguiente') private campoSiguiente?: ElementRef<HTMLInputElement>;

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
    this.cargarMovimientos();
    this.escucharBusquedaPorNombre();
    // El motivo se muestra con el texto del catálogo, no con el código:
    // "VEHICULO_INACTIVO" no es algo que se le pueda leer a nadie de noche.
    this.admin.parametros('MOTIVO_DENEGACION').subscribe(m => (this.motivos = m));
  }

  /**
   * El campo recupera el foco cada vez que vuelve a existir.
   *
   * <p>No basta el atributo `autofocus`: el campo se destruye al abrir una
   * ficha y vuelve a crearse al cerrarla, y el navegador solo lo honra al
   * cargar la página. Sin esto, el guardia pasa el segundo código por el
   * lector y no pasa nada — el lector teclea, pero no hay dónde.</p>
   *
   * <p>Solo se toma el foco cuando NADIE lo tiene. Reclamarlo siempre se lo
   * quitaría al guardia en cuanto tocara el botón de paso automático o
   * cualquier otro control, que es peor que el problema que resuelve.</p>
   *
   * <p>Y se toma SELECCIONANDO lo que haya escrito. Cancelar una ficha deja el
   * texto anterior a propósito —para corregir un dígito mal tecleado sin
   * volver a escribirlo entero— pero el lector teclea al final de lo que
   * encuentre: sin seleccionar, el siguiente escaneo se pegaría detrás del
   * documento anterior y saldría un número que no es de nadie. Seleccionado,
   * el escaneo lo reemplaza, y el guardia que hace clic para corregir no pasa
   * por aquí y conserva su cursor.</p>
   */
  ngAfterViewChecked(): void {
    const activo = document.activeElement;
    const nadieEscribe = !activo || activo === document.body;
    if (!nadieEscribe) {
      return;
    }
    // Con la ficha abierta el que recibe al siguiente de la fila es el campo
    // del pie, no el de la pantalla de escaneo — que ni siquiera existe.
    const campo = this.campoCodigo?.nativeElement ?? this.campoSiguiente?.nativeElement;
    if (!campo) {
      return;
    }
    campo.focus();
    // La selección va un turno después: al recrearse el campo, ngModel escribe
    // el valor en un ciclo posterior y deja el cursor al final, deshaciendo un
    // select() hecho aquí mismo. Comprobado — no es precaución teórica.
    setTimeout(() => campo.select());
  }

  private cargarPresencia(): void {
    this.acceso.presencia().subscribe({
      next: presencia => (this.presencia = presencia),
      error: () => undefined
    });
  }

  // ── Últimos movimientos ──────────────────────────────────────────────────
  //
  // Llenan el vacío que dejaba la pantalla y, sobre todo, le dejan al guardia
  // ver lo que acaba de pasar: si registró a alguien en el sentido equivocado
  // o le abrió a quien no era, lo ve en el renglón de arriba y no dos días
  // después en la bitácora.
  //
  // Solo se pintan mientras NO hay ficha: la decisión no compite con nada.

  ultimosMovimientos: AccesoEvento[] = [];

  /**
   * Seis. No es un número técnico: es lo que cabe sin empujar el campo de
   * escaneo fuera de la pantalla de una tablet, que es la regla de esta
   * pantalla. Para más está la bitácora, que pagina y filtra.
   */
  private static readonly CUANTOS_MOVIMIENTOS = 6;

  private cargarMovimientos(): void {
    this.acceso.eventos({ tamano: EscanerComponent.CUANTOS_MOVIMIENTOS })
      .subscribe({
        next: pagina => (this.ultimosMovimientos = pagina.content ?? []),
        // Un panel de contexto que no carga no puede romper la portería: se
        // queda vacío y el guardia sigue escaneando.
        error: () => undefined
      });
  }

  ngOnDestroy(): void {
    this.detenerCuenta();
    this.suscripcionBusqueda?.unsubscribe();
  }

  /**
   * Alguien nuevo pasó su código por el lector con una ficha todavía en pantalla.
   *
   * <p>Se registra la anterior con el default y se pasa a la nueva. Sin esto,
   * el que llega tendría que esperar a que se agote la cuenta del que ya se
   * fue.</p>
   */
  private alLlegarOtro(lectura: string): void {
    // El mismo código pasado dos veces no es alguien nuevo. Y sin ficha
    // permitida no hay nada que registrar solo — un rechazo se resuelve
    // mirándolo, no dejando que lo empujen.
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
   * El único punto de entrada: lector de QR, lector de código de barras o
   * cédula tecleada.
   *
   * <p>Los dos lectores son teclados — escriben lo que leen en el campo que
   * tenga el foco—, así que aquí puede caer el QR de GUARDIAN o el código de
   * barras de una cédula. El prefijo distingue los dos mundos sin preguntarle
   * nada al guardia, que es quien menos tiempo tiene para responder
   * preguntas.</p>
   */
  verificarEntradaManual(): void {
    const texto = this.entradaManual.trim();
    if (!texto) {
      return;
    }
    // El lector repite la misma lectura si el código se queda al frente, y el
    // guardia puede pasarlo dos veces. No se limpia el campo: si esto dispara
    // es porque ya se proceso, y borrarlo ahora seria justo el problema que se
    // corrigio antes — que el guardia pierda lo que escribio sin haber
    // terminado nada nuevo.
    if (this.enEnfriamiento(texto)) {
      return;
    }

    // El lector teclea igual sobre una ficha abierta que sobre la pantalla de
    // escaneo. Si ya hay alguien en pantalla, el que acaba de llegar lo empuja.
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

  // ── Respaldo: buscar por nombre ──────────────────────────────────────────
  //
  // Para cuando el lector no resuelve: el QR no carga, el teléfono se quedó sin
  // batería, la persona llegó sin cédula.
  //
  // SIN un modo que el guardia tenga que elegir, igual que el resto de esta
  // pantalla. Lo que sale del lector es numérico (una cédula) o empieza por
  // GRD (un código), así que en cuanto aparecen letras es que alguien está
  // tecleando un nombre — y solo entonces se busca.

  candidatos: CandidatoGarita[] = [];
  buscandoCandidatos = false;

  private readonly nombreTecleado = new Subject<string>();
  private suscripcionBusqueda?: Subscription;

  private escucharBusquedaPorNombre(): void {
    this.suscripcionBusqueda = this.nombreTecleado
      .pipe(
        // 250ms: por debajo se dispara una consulta por letra; por encima, el
        // guardia ya levantó la vista esperando la lista.
        debounceTime(250),
        distinctUntilChanged(),
        switchMap(texto => this.acceso.buscarCandidatos(texto))
      )
      .subscribe({
        next: encontrados => {
          this.candidatos = encontrados;
          this.buscandoCandidatos = false;
        },
        error: () => {
          this.candidatos = [];
          this.buscandoCandidatos = false;
        }
      });
  }

  /** Cada tecla del campo. El lector también pasa por aquí, y no dispara nada. */
  alEscribir(): void {
    const texto = this.entradaManual.trim();
    const esNombre = texto.length >= 3
      && !/^\d+$/.test(texto)
      && !texto.toUpperCase().startsWith(PREFIJO_CODIGO);

    if (!esNombre) {
      this.candidatos = [];
      this.buscandoCandidatos = false;
      return;
    }
    this.buscandoCandidatos = true;
    this.nombreTecleado.next(texto);
  }

  /**
   * El guardia eligió a alguien de la lista.
   *
   * <p>Se verifica por su DOCUMENTO y no por su id: es el mismo camino que
   * sigue una cédula tecleada, así que la decisión la toma exactamente la
   * misma lógica. Encontrar a alguien en esta lista no lo autoriza.</p>
   */
  elegirCandidato(candidato: CandidatoGarita): void {
    this.candidatos = [];
    this.entradaManual = '';
    this.procesando = true;
    this.verificarDocumento(candidato.documento);
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
          this.cargarMovimientos();

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
    // El foco vuelve solo al campo en ngAfterViewChecked: el lector necesita
    // dónde escribir antes de que llegue el siguiente.
  }

  /**
   * Si esta lectura es la misma credencial que se acaba de registrar y el
   * enfriamiento todavía no pasó. Compara contra payload Y documento porque la
   * misma persona puede volver a pasar el QR o la cédula.
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
      return 'cancel';
    }
    return this.soloSalida ? 'warning' : 'check_circle';
  }

  get textoSentido(): string {
    const sentido: Sentido | null | undefined =
      this.sentidoCorregido ?? this.ficha?.sentidoSugerido;
    return sentido === 'S' ? 'SALIDA' : 'ENTRADA';
  }

  /** La flecha del sentido: entra hacia adentro, sale hacia afuera. */
  get iconoSentido(): string {
    return this.textoSentido === 'SALIDA' ? 'logout' : 'login';
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
