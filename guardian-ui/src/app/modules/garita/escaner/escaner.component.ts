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

const ID_LECTOR = 'gd-lector';

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

  /** Alternativa cuando la cámara falla o la tablet no tiene uno decente. */
  modoManual = false;
  payloadManual = '';

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

  private lector: Html5Qrcode | null = null;
  private procesando = false;

  constructor(
    private readonly acceso: AccesoService,
    private readonly admin: AdminService,
    private readonly fotoService: FotoService
  ) {}

  ngOnInit(): void {
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
        this.modoManual = true;
        this.error = 'No pudimos abrir la cámara. Escribe el código a mano.';
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
    // La cámara sigue leyendo el mismo QR mientras esté al frente. Sin esta
    // guarda se dispararían decenas de peticiones por un solo escaneo.
    if (this.procesando) {
      return;
    }
    this.procesando = true;
    this.detenerCamara();
    this.verificar(payload);
  }

  // ── Flujo ────────────────────────────────────────────────────────────────

  verificarManual(): void {
    if (!this.payloadManual.trim()) {
      return;
    }
    this.procesando = true;
    this.verificar(this.payloadManual.trim());
  }

  private verificar(payload: string): void {
    this.etapa = 'verificando';
    this.error = null;

    this.acceso.verificar(payload).subscribe({
      next: ficha => {
        this.ficha = { ...ficha, payload: ficha.payload ?? payload };
        this.etapa = 'ficha';
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
          // El registro puede volver DENEGADO con 200: entre el escaneo y el
          // toque pasan segundos, y en esos segundos el administrador pudo
          // deshabilitar el carro o revocar la credencial. Antes se pintaba
          // verde igual — el guardia dejaba pasar justo el caso que motivó el
          // bloqueo. Ahora el veredicto sale del evento, no del código HTTP.
          this.eventoRegistrado = evento;
          this.etapa = 'registrado';
          this.cargarPresencia();
          // Vuelve solo a escanear: en hora pico nadie va a tocar "siguiente".
          // El rojo se queda más tiempo: hay que leer POR QUÉ no entra.
          setTimeout(() => this.reiniciar(), this.permitido ? 2000 : 5000);
        },
        error: (fallo: HttpErrorResponse) => {
          this.registrando = false;
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar el ingreso.';
        }
      });
  }

  /** Un toque alterna; el sistema ya acertó casi siempre, esto es la excepción. */
  alternarSentido(): void {
    const sugerido = this.ficha?.sentidoSugerido ?? 'E';
    if (this.sentidoCorregido === null) {
      this.sentidoCorregido = sugerido === 'E' ? 'S' : 'E';
    } else {
      this.sentidoCorregido = null;
    }
  }

  reiniciar(): void {
    this.ficha = null;
    this.payloadManual = '';
    this.procesando = false;
    this.registrando = false;
    this.sentidoCorregido = null;
    this.eventoRegistrado = null;
    this.etapa = 'escaneando';

    if (!this.modoManual) {
      this.iniciarCamara();
    }
  }

  alternarManual(): void {
    this.modoManual = !this.modoManual;
    this.error = null;

    if (this.modoManual) {
      this.detenerCamara();
    } else {
      this.iniciarCamara();
    }
  }

  // ── Presentación ─────────────────────────────────────────────────────────

  get textoSentido(): string {
    const sentido: Sentido | null | undefined =
      this.sentidoCorregido ?? this.ficha?.sentidoSugerido;
    return sentido === 'S' ? 'SALIDA' : 'ENTRADA';
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
