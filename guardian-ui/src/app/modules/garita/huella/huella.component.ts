import { Component, EventEmitter, Output } from '@angular/core';

import { AccesoService } from '../../../core/services/acceso.service';
import { CandidatoGarita } from '../../../core/models/acceso.model';

/** En qué punto del módulo está el guardia. */
type Paso = 'inicio' | 'buscando-persona' | 'eligiendo-dedo' | 'capturando';

/**
 * El módulo de huella de la portería.
 *
 * <p><b>Por qué vive aparte del escáner.</b> Código y documento son el mismo
 * camino —un texto que el sistema resuelve— y por eso comparten campo. La
 * huella no: tiene dos acciones distintas (identificar y registrar), su propio
 * recorrido de tres pasos y un aparato que puede no estar. Meterlo en el
 * escáner habría duplicado su tamaño para algo que no comparte nada.</p>
 *
 * <p><b>Qué funciona hoy.</b> Todo menos tocar el sensor. Encontrar a la
 * persona por su cédula opera de verdad contra el sistema; la captura dice que
 * no hay sensor en vez de fingir. Cuando llegue el lector, lo único que se
 * llena es la llamada al SDK.</p>
 */
@Component({
  selector: 'gd-huella',
  templateUrl: './huella.component.html',
  styleUrl: './huella.component.scss',
  standalone: false
})
export class HuellaComponent {

  /**
   * El lector todavía no existe.
   *
   * <p>Un navegador NO puede leer un sensor biométrico USB por su cuenta: hace
   * falta el servicio local del fabricante escuchando en localhost. Mientras
   * no esté, esto es false y la pantalla lo dice.</p>
   */
  readonly sensorConectado = false;

  /**
   * Dos dedos por persona. No es un límite técnico: es que quien registra
   * cuatro dedos nunca se acuerda de cuál puso, y con dos —uno de cada mano—
   * queda cubierto el día que llegue con una curita.
   */
  readonly maximoDedos = 2;

  /**
   * Tres capturas del MISMO dedo. El SDK las fusiona en una sola plantilla:
   * no son tres huellas, son tres lecturas para que la plantilla resultante
   * aguante que el dedo venga torcido, seco o sucio.
   */
  readonly capturasPorDedo = 3;

  paso: Paso = 'inicio';
  documento = '';
  persona: CandidatoGarita | null = null;
  dedoElegido: 'DERECHO' | 'IZQUIERDO' | null = null;
  capturaActual = 0;
  error: string | null = null;
  buscando = false;

  /** El guardia quiere identificar a quien está enfrente. */
  @Output() identificar = new EventEmitter<void>();

  constructor(private readonly acceso: AccesoService) {}

  // ── Registrar ────────────────────────────────────────────────────────────

  empezarRegistro(): void {
    this.paso = 'buscando-persona';
    this.documento = '';
    this.persona = null;
    this.dedoElegido = null;
    this.capturaActual = 0;
    this.error = null;
  }

  /**
   * Paso 1: quién es. Se busca por documento y no por nombre: enrolar a la
   * persona equivocada deja una huella que abre la puerta a nombre de otro, y
   * eso no se descubre hasta que pasa algo.
   */
  buscarPersona(): void {
    const doc = this.documento.trim();
    if (!doc || this.buscando) {
      return;
    }
    this.buscando = true;
    this.error = null;

    this.acceso.buscarCandidatos(doc).subscribe({
      next: encontrados => {
        this.buscando = false;
        const exacta = encontrados.find(c => c.documento === doc);
        if (!exacta) {
          this.error = 'No encontramos a nadie con ese documento.';
          return;
        }
        this.persona = exacta;
        this.paso = 'eligiendo-dedo';
      },
      error: () => {
        this.buscando = false;
        this.error = 'No pudimos buscar. Intenta de nuevo.';
      }
    });
  }

  /** Paso 2: cuál dedo. */
  elegirDedo(dedo: 'DERECHO' | 'IZQUIERDO'): void {
    this.dedoElegido = dedo;
    this.capturaActual = 0;
    this.paso = 'capturando';
  }

  volver(): void {
    if (this.paso === 'capturando') {
      this.paso = 'eligiendo-dedo';
      return;
    }
    if (this.paso === 'eligiendo-dedo') {
      this.paso = 'buscando-persona';
      return;
    }
    this.paso = 'inicio';
    this.error = null;
  }

  cancelar(): void {
    this.paso = 'inicio';
    this.documento = '';
    this.persona = null;
    this.dedoElegido = null;
    this.capturaActual = 0;
    this.error = null;
  }
}
