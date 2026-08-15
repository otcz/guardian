import { Component, EventEmitter, OnInit, Output } from '@angular/core';

import { AccesoService } from '../../../core/services/acceso.service';
import { LectorHuellaService } from '../../../core/services/lector-huella.service';
import { CandidatoGarita } from '../../../core/models/acceso.model';

/** En qué punto del módulo está el guardia. */
type Paso = 'inicio' | 'buscando-persona' | 'eligiendo-dedo' | 'capturando' | 'listo';

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
export class HuellaComponent implements OnInit {

  /**
   * Hay lector y su servicio local responde.
   *
   * <p>Se resuelve preguntando a DOS partes, y las dos tienen que decir que
   * sí: el navegador —que necesita el servicio local del fabricante escuchando
   * en localhost— y el servidor, que necesita el algoritmo de cotejo. Con una
   * sola de las dos, enrolar produce plantillas que nadie va a poder
   * comparar.</p>
   */
  sensorConectado = false;

  /**
   * Los límites los manda el SERVIDOR, no esta pantalla.
   *
   * <p>Son reglas de negocio —cuántos dedos, cuántas capturas— y si viven en
   * dos sitios el día que cambien van a quedar en desacuerdo. Estos valores
   * son solo el respaldo mientras llega la respuesta.</p>
   */
  maximoDedos = 2;
  capturasPorDedo = 3;

  /**
   * Las dos manos, cada una con sus cinco dedos.
   *
   * <p>Se nombra el dedo EXACTO y no solo la mano: si alguien registró el
   * índice y el día de la curita pone el pulgar, hay que poder decirle "pon el
   * índice derecho" en vez de "pon el de la derecha". Sin el nombre, ese
   * momento se resuelve a ensayo y error con la fila esperando.</p>
   *
   * <p>El orden es el de la mano vista de frente, del pulgar al meñique, para
   * que la pantalla se lea como se mira una mano.</p>
   */
  readonly manos = [
    {
      mano: 'Izquierda',
      dedos: [
        { codigo: 'IZQ_PULGAR', nombre: 'Pulgar' },
        { codigo: 'IZQ_INDICE', nombre: 'Índice' },
        { codigo: 'IZQ_MEDIO', nombre: 'Medio' },
        { codigo: 'IZQ_ANULAR', nombre: 'Anular' },
        { codigo: 'IZQ_MENIQUE', nombre: 'Meñique' }
      ]
    },
    {
      mano: 'Derecha',
      dedos: [
        { codigo: 'DER_PULGAR', nombre: 'Pulgar' },
        { codigo: 'DER_INDICE', nombre: 'Índice' },
        { codigo: 'DER_MEDIO', nombre: 'Medio' },
        { codigo: 'DER_ANULAR', nombre: 'Anular' },
        { codigo: 'DER_MENIQUE', nombre: 'Meñique' }
      ]
    }
  ];

  paso: Paso = 'inicio';
  documento = '';
  persona: CandidatoGarita | null = null;
  dedoElegido: string | null = null;
  capturaActual = 0;
  error: string | null = null;
  buscando = false;
  guardando = false;

  /** Los dedos que la persona ya tiene, para no ofrecer el mismo dos veces. */
  yaRegistrados: string[] = [];

  /** El guardia quiere identificar a quien está enfrente. */
  @Output() identificar = new EventEmitter<void>();

  constructor(
    private readonly acceso: AccesoService,
    private readonly lector: LectorHuellaService
  ) {}

  ngOnInit(): void {
    // Las dos mitades: el lector del lado del navegador y el cotejo del lado
    // del servidor. Sin ambas, enrolar no sirve de nada.
    this.acceso.estadoHuella().subscribe({
      next: estado => {
        this.maximoDedos = estado.maximoDedos;
        this.capturasPorDedo = estado.capturasPorDedo;
        this.lector.disponible().subscribe(
          hayLector => (this.sensorConectado = hayLector && estado.disponible));
      },
      error: () => (this.sensorConectado = false)
    });
  }

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
        this.cargarDedos(exacta.personaId);
      },
      error: () => {
        this.buscando = false;
        this.error = 'No pudimos buscar. Intenta de nuevo.';
      }
    });
  }

  private cargarDedos(personaId: number): void {
    this.acceso.huellasDe(personaId).subscribe({
      next: huellas => (this.yaRegistrados = huellas.dedos.map(d => d.dedo)),
      // Que no se sepa qué dedos tiene no impide registrar: en el peor caso el
      // servidor reemplaza el que ya estaba, que es lo correcto igual.
      error: () => (this.yaRegistrados = [])
    });
  }

  yaTiene(dedo: string): boolean {
    return this.yaRegistrados.includes(dedo);
  }

  /** "IZQ_INDICE" -> "índice izquierdo", para poder pedírselo a la persona. */
  nombreDe(codigo: string | null): string {
    if (!codigo) {
      return '';
    }
    for (const mano of this.manos) {
      const dedo = mano.dedos.find(d => d.codigo === codigo);
      if (dedo) {
        return `${dedo.nombre.toLowerCase()} ${mano.mano.toLowerCase()}`;
      }
    }
    return codigo;
  }

  /** Paso 2: cuál dedo. */
  elegirDedo(dedo: string): void {
    this.dedoElegido = dedo;
    this.capturaActual = 0;
    this.error = null;
    this.paso = 'capturando';
  }

  /**
   * Paso 3: las tres lecturas del mismo dedo.
   *
   * <p>Se piden UNA a una y en secuencia, no en paralelo: el sensor tiene un
   * solo dedo encima y entre lectura y lectura la persona tiene que levantarlo
   * y volverlo a poner — de eso salen las tres vistas distintas que hacen buena
   * a la plantilla.</p>
   */
  capturar(): void {
    if (!this.persona || !this.dedoElegido || this.guardando) {
      return;
    }
    this.error = null;
    this.guardando = true;
    this.lecturas = [];
    this.siguienteLectura();
  }

  private lecturas: string[] = [];
  private ultimaCalidad?: number;

  private siguienteLectura(): void {
    this.lector.capturar().subscribe({
      next: lectura => {
        this.lecturas.push(lectura.plantilla);
        this.ultimaCalidad = lectura.calidad;
        this.capturaActual = this.lecturas.length;

        if (this.lecturas.length < this.capturasPorDedo) {
          this.siguienteLectura();
          return;
        }
        this.guardar();
      },
      error: fallo => {
        this.guardando = false;
        this.capturaActual = 0;
        this.error = fallo?.message ?? 'No pudimos leer la huella.';
      }
    });
  }

  private guardar(): void {
    this.acceso.registrarHuella({
      personaId: this.persona!.personaId,
      dedo: this.dedoElegido!,
      lecturas: this.lecturas,
      calidad: this.ultimaCalidad
    }).subscribe({
      next: huellas => {
        this.guardando = false;
        this.yaRegistrados = huellas.dedos.map(d => d.dedo);
        this.paso = 'listo';
      },
      error: fallo => {
        this.guardando = false;
        this.capturaActual = 0;
        this.error = fallo?.error?.mensaje ?? 'No pudimos guardar la huella.';
      }
    });
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
