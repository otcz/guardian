import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Html5Qrcode } from 'html5-qrcode';

import { AccesoService } from '../../../core/services/acceso.service';
import { FotoService } from '../../../core/services/foto.service';
import {
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

  private lector: Html5Qrcode | null = null;
  private procesando = false;

  constructor(
    private readonly acceso: AccesoService,
    private readonly fotoService: FotoService
  ) {}

  ngOnInit(): void {
    this.cargarPresencia();
    this.iniciarCamara();
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
    if (!this.ficha?.payload) {
      return;
    }

    this.acceso
      .registrar({
        payload: this.ficha.payload,
        modo,
        vehiculoId: vehiculo?.id ?? null,
        sentido: this.ficha.sentidoSugerido
      })
      .subscribe({
        next: () => {
          this.etapa = 'registrado';
          this.cargarPresencia();
          // Vuelve solo a escanear: en hora pico nadie va a tocar "siguiente".
          setTimeout(() => this.reiniciar(), 2000);
        },
        error: (fallo: HttpErrorResponse) => {
          this.error = fallo.error?.mensaje ?? 'No pudimos registrar el ingreso.';
        }
      });
  }

  reiniciar(): void {
    this.ficha = null;
    this.payloadManual = '';
    this.procesando = false;
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
    const sentido: Sentido | null | undefined = this.ficha?.sentidoSugerido;
    return sentido === 'S' ? 'SALIDA' : 'ENTRADA';
  }

  get tieneVehiculos(): boolean {
    return (this.ficha?.vehiculos?.length ?? 0) > 0;
  }

  get fotoFicha(): string | null {
    return this.fotoService.urlAbsoluta(this.ficha?.fotoUrl ?? null);
  }
}
