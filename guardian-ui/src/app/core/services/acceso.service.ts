import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AccesoEvento,
  CandidatoGarita,
  EstadoHuella,
  HuellasDeUnaPersona,
  FichaVerificacion,
  MiQr,
  Pagina,
  Presencia,
  RegistrarAccesoRequest,
  Resultado
} from '../models/acceso.model';
import { PorteriaActivaService } from './porteria-activa.service';

@Injectable({ providedIn: 'root' })
export class AccesoService {

  private readonly base = `${environment.apiUrl}/acceso`;

  constructor(
    private readonly http: HttpClient,
    private readonly porteriaActiva: PorteriaActivaService
  ) {}

  presencia(): Observable<Presencia> {
    return this.http.get<Presencia>(`${this.base}/presencia`);
  }

  /**
   * Candidatos por nombre, para cuando el lector no resuelve.
   *
   * <p>El texto va como parámetro y no en la ruta a propósito: un nombre propio
   * dentro de una URL termina escrito en los registros de Cloudflare y de
   * Caddy.</p>
   */
  buscarCandidatos(texto: string): Observable<CandidatoGarita[]> {
    return this.http.get<CandidatoGarita[]>(`${this.base}/buscar`, {
      params: new HttpParams().set('q', texto)
    });
  }

  /**
   * La portería de ESTA tablet viaja en cada llamada.
   *
   * <p>Se pone acá y no en cada componente para que no exista forma de mandar
   * una verificación sin decir por dónde: con dos porterías activas, el evento
   * que no la trae queda sin punto de acceso y la bitácora deja de poder
   * responder por dónde pasó nadie.</p>
   */
  private get puntoAccesoId(): number | null {
    return this.porteriaActiva.id;
  }

  verificar(payload: string): Observable<FichaVerificacion> {
    return this.http.post<FichaVerificacion>(
      `${this.base}/verificar`, { payload, puntoAccesoId: this.puntoAccesoId });
  }

  /**
   * El otro camino a la misma ficha: por documento y no por QR.
   *
   * <p>Lo usan el lector de código de barras —del PDF417 de una cédula sale el
   * número, no un payload— y la cédula tecleada. También responde por un
   * invitado que llega sin su código y entrega la cédula.</p>
   */
  verificarPorDocumento(documento: string): Observable<FichaVerificacion> {
    return this.http.post<FichaVerificacion>(
      `${this.base}/verificar-documento`, { documento, puntoAccesoId: this.puntoAccesoId });
  }

  registrar(request: RegistrarAccesoRequest): Observable<AccesoEvento> {
    return this.http.post<AccesoEvento>(
      `${this.base}/registrar`, { ...request, puntoAccesoId: this.puntoAccesoId });
  }

  /**
   * La bitácora, filtrada EN EL SERVIDOR.
   *
   * <p>No se filtra en memoria como en las otras tablas: esta pagina, y un
   * filtro sobre la página a la vista diría "no hay nada" con el evento
   * esperando en la página siguiente.</p>
   */
  eventos(filtros: {
    resultados?: Resultado[];
    sentidos?: string[];
    modos?: string[];
    motivos?: string[];
    porteriaIds?: number[];
    texto?: string | null;
    casaId?: number | null;
    pagina?: number;
    tamano?: number;
  } = {}): Observable<Pagina<AccesoEvento>> {

    let params = new HttpParams()
      .set('pagina', String(filtros.pagina ?? 0))
      .set('tamano', String(filtros.tamano ?? 50));

    // Repetido, no separado por comas: es como Spring arma una List<> desde el
    // query string, y una coma dentro de un valor no rompe nada.
    const repetir = (campo: string, valores?: (string | number)[]) => {
      for (const valor of valores ?? []) {
        params = params.append(campo, String(valor));
      }
    };
    repetir('resultados', filtros.resultados);
    repetir('sentidos', filtros.sentidos);
    repetir('modos', filtros.modos);
    repetir('motivos', filtros.motivos);
    repetir('porteriaIds', filtros.porteriaIds);

    if (filtros.texto?.trim()) {
      params = params.set('texto', filtros.texto.trim());
    }
    if (filtros.casaId) {
      params = params.set('casaId', String(filtros.casaId));
    }

    return this.http.get<Pagina<AccesoEvento>>(`${this.base}/eventos`, { params });
  }

  miQr(): Observable<MiQr> {
    return this.http.get<MiQr>(`${environment.apiUrl}/residente/mi-qr`);
  }

  // ── Huellas ──────────────────────────────────────────────────────────────
  //
  // El enrolamiento. El cotejo, cuando exista, entra por verificar(): la
  // decisión de dejar pasar tiene que salir de un solo sitio.

  /** Si hay lector configurado del lado del servidor, y con qué algoritmo. */
  estadoHuella(): Observable<EstadoHuella> {
    return this.http.get<EstadoHuella>(`${this.base}/huellas/estado`);
  }

  huellasDe(personaId: number): Observable<HuellasDeUnaPersona> {
    return this.http.get<HuellasDeUnaPersona>(`${this.base}/huellas/${personaId}`);
  }

  /** Las tres lecturas de UN dedo. El servidor las funde en una plantilla. */
  registrarHuella(peticion: {
    personaId: number;
    dedo: string;
    lecturas: string[];
    calidad?: number;
  }): Observable<HuellasDeUnaPersona> {
    return this.http.post<HuellasDeUnaPersona>(`${this.base}/huellas`, peticion);
  }

  eliminarHuella(personaId: number, dedo: string): Observable<HuellasDeUnaPersona> {
    return this.http.delete<HuellasDeUnaPersona>(
      `${this.base}/huellas/${personaId}/${dedo}`);
  }
}
