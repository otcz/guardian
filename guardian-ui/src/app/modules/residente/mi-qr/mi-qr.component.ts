import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AccesoService } from '../../../core/services/acceso.service';
import { ResidenteService } from '../../../core/services/residente.service';
import { LLAVE_CACHE_MI_QR as LLAVE_CACHE, MiQr } from '../../../core/models/acceso.model';

/** Tope del QR en pantalla. Más allá no gana legibilidad y sí recorte. */
const ANCHO_MAXIMO_QR = 320;
const PROPORCION_QR = 0.72;

/** En modo sol el código ocupa lo que da la pantalla, con un margen mínimo. */
const ANCHO_MAXIMO_SOL = 400;
const MARGEN_SOL_PX = 32;

/**
 * La credencial del residente.
 *
 * <p>El QR se dibuja en el teléfono a partir del payload, y el payload se
 * guarda en el dispositivo. Eso es a propósito: la portería es justo donde peor
 * señal hay, y una credencial que solo funciona con datos móviles deja a la
 * persona parada en la entrada. Al ser un QR permanente y firmado, el payload
 * cacheado sigue siendo válido.</p>
 */
@Component({
  selector: 'gd-mi-qr',
  templateUrl: './mi-qr.component.html',
  styleUrl: './mi-qr.component.scss',
  standalone: false
})
export class MiQrComponent implements OnInit, OnDestroy {

  credencial: MiQr | null = null;
  cargando = true;
  desdeCache = false;
  error: string | null = null;

  /** Pantalla completa en blanco: el QR más grande y con más contraste. */
  sol = false;

  /** Ancho calculado acá, no en la plantilla (regla de CLAUDE.md §2). */
  anchoQr = ANCHO_MAXIMO_QR;

  /** Bloqueo de apagado de pantalla mientras se hace la fila. */
  private bloqueoPantalla: WakeLockSentinel | null = null;

  private readonly alVolver = () => {
    if (this.sol && !this.bloqueoPantalla) {
      this.pedirBloqueoPantalla();
    }
  };

  constructor(
    private readonly acceso: AccesoService,
    private readonly residente: ResidenteService
  ) {}

  ngOnInit(): void {
    this.anchoQr = this.anchoParaElModo();
    this.credencial = this.leerCache();

    this.acceso.miQr().subscribe({
      next: credencial => {
        this.credencial = credencial;
        this.desdeCache = false;
        this.cargando = false;
        this.guardarEnCache(credencial);
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando = false;
        if (this.credencial) {
          // Hay copia local: la credencial se muestra igual y solo se avisa
          // que no se pudo confirmar contra el servidor.
          this.desdeCache = true;
        } else {
          this.error = fallo.error?.mensaje ?? 'No pudimos cargar tu código.';
        }
      }
    });

    document.addEventListener('visibilitychange', this.alVolver);
  }

  ngOnDestroy(): void {
    document.removeEventListener('visibilitychange', this.alVolver);
    this.liberarBloqueoPantalla();
  }

  subiendoFoto = false;

  /**
   * El payload ya resuelto para la librería del QR, que exige un string.
   * Cuando falta la foto no hay código y esa rama de la plantilla ni se
   * dibuja; el vacío es solo para que el tipo cuadre.
   */
  get payloadQr(): string {
    return this.credencial?.payload ?? '';
  }

  /**
   * El residente sube su propia foto y el backend le emite la credencial en
   * la misma respuesta: sube la foto y ya tiene su código, sin recargar y sin
   * depender de que el administrador se acuerde.
   */
  guardarMiFoto(fotoUrl: string | null): void {
    if (!fotoUrl || this.subiendoFoto) {
      return;
    }

    this.subiendoFoto = true;
    this.error = null;

    this.residente.fijarMiFoto(fotoUrl).subscribe({
      next: credencial => {
        this.subiendoFoto = false;
        this.credencial = credencial;
        this.guardarEnCache(credencial);
      },
      error: (fallo: HttpErrorResponse) => {
        this.subiendoFoto = false;
        this.error = fallo.error?.mensaje ?? 'No pudimos guardar tu foto.';
      }
    });
  }

  /**
   * Sin credencial no hay nada que cachear: guardar una ficha con
   * necesitaFoto dejaría al residente viendo "falta tu foto" sin conexión
   * aunque ya la hubiera subido.
   */
  private guardarEnCache(credencial: MiQr): void {
    if (credencial.necesitaFoto || !credencial.payload) {
      localStorage.removeItem(LLAVE_CACHE);
      return;
    }
    localStorage.setItem(LLAVE_CACHE, JSON.stringify(credencial));
  }

  /**
   * Un toque sobre el código lo lleva a pantalla completa sobre blanco y pide
   * que la pantalla no se apague. Se sale tocando en cualquier punto: quien
   * entra por accidente no puede quedar encerrado.
   */
  alternarSol(): void {
    this.sol = !this.sol;

    // Se REDIBUJA el código al tamaño nuevo. Estirarlo por CSS lo interpola y
    // convierte los bordes nítidos de los módulos en degradados grises —
    // justo en la pantalla hecha para leerse bajo el sol.
    this.anchoQr = this.anchoParaElModo();

    if (this.sol) {
      // Tiene que ir en el MISMO gesto del usuario: la API lo exige.
      this.pedirBloqueoPantalla();
    } else {
      this.liberarBloqueoPantalla();
    }
  }

  private anchoParaElModo(): number {
    return this.sol
      ? Math.min(window.innerWidth - MARGEN_SOL_PX, ANCHO_MAXIMO_SOL)
      : Math.min(Math.round(window.innerWidth * PROPORCION_QR), ANCHO_MAXIMO_QR);
  }

  private pedirBloqueoPantalla(): void {
    // Sin soporte (iOS < 16.4) o sin HTTPS simplemente no pasa nada: jamás
    // debe impedir que el QR se muestre.
    navigator.wakeLock?.request('screen')
      .then(bloqueo => (this.bloqueoPantalla = bloqueo))
      .catch(() => undefined);
  }

  private liberarBloqueoPantalla(): void {
    this.bloqueoPantalla?.release().catch(() => undefined);
    this.bloqueoPantalla = null;
  }

  private leerCache(): MiQr | null {
    const crudo = localStorage.getItem(LLAVE_CACHE);
    if (!crudo) {
      return null;
    }
    try {
      return JSON.parse(crudo) as MiQr;
    } catch {
      localStorage.removeItem(LLAVE_CACHE);
      return null;
    }
  }
}
