import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface PorteriaGarita {
  id: number;
  nombre: string;
  direccion: string | null;
  permiteVehiculo: string;
  /** Si el guardia que pregunta está asignado a esta portería. */
  asignada: boolean;
}

export interface PorteriasGarita {
  porterias: PorteriaGarita[];
  /** La que se propone, o null si no hay una clara. Es sugerencia, no candado. */
  sugeridaId: number | null;
}

/** Lo mínimo para recordar dónde está esta tablet, más la sede que lo decidió. */
interface PorteriaGuardada {
  id: number;
  nombre: string;
  sedeId: number | null;
}

const LLAVE = 'guardian.porteria';

/**
 * En qué portería está ESTA tablet.
 *
 * <p>Vive en el dispositivo y no en la cuenta a propósito: el aparato es el que
 * está atornillado a la puerta, las personas son las que rotan de turno. Un
 * guardia que cubre la otra puerta no debería tener que reconfigurar nada, y
 * el que llega a relevarlo tampoco.</p>
 *
 * <p>Se guarda también la sede: si la tablet cambia de conjunto —o un super
 * administrador entra a otra sede sin cerrar sesión— la elección anterior deja
 * de valer. Sin esa comprobación se estamparía la portería de una sede sobre
 * los eventos de otra.</p>
 */
@Injectable({ providedIn: 'root' })
export class PorteriaActivaService {

  private readonly base = `${environment.apiUrl}/acceso`;

  constructor(private readonly http: HttpClient) {}

  opciones(): Observable<PorteriasGarita> {
    return this.http.get<PorteriasGarita>(`${this.base}/porterias`);
  }

  /**
   * Resuelve qué mostrarle al guardia al abrir la consola.
   *
   * @returns las opciones, y si hay que preguntarle o no.
   */
  resolver(sedeId: number | null): Observable<{
    opciones: PorteriaGarita[];
    elegida: PorteriaGarita | null;
    preguntar: boolean;
  }> {
    return this.opciones().pipe(map(respuesta => {
      const opciones = respuesta.porterias ?? [];

      // Una sola portería activa: preguntarle entre una única opción es un
      // toque que no decide nada.
      if (opciones.length === 1) {
        this.fijar(opciones[0], sedeId);
        return { opciones, elegida: opciones[0], preguntar: false };
      }

      const guardada = this.leer();
      const vigente = guardada && guardada.sedeId === sedeId
        ? opciones.find(o => o.id === guardada.id) ?? null
        : null;

      if (vigente) {
        return { opciones, elegida: vigente, preguntar: false };
      }

      // NO se auto-elige otra: la guardada dejó de existir o es de otra sede, y
      // cambiar de puerta en silencio es peor que no saber. Un nulo se lee
      // después como "no se supo"; un nombre equivocado se lee como un hecho.
      this.olvidar();
      return { opciones, elegida: null, preguntar: true };
    }));
  }

  fijar(porteria: PorteriaGarita, sedeId: number | null): void {
    const dato: PorteriaGuardada = { id: porteria.id, nombre: porteria.nombre, sedeId };
    localStorage.setItem(LLAVE, JSON.stringify(dato));
  }

  olvidar(): void {
    localStorage.removeItem(LLAVE);
  }

  /** El id que la consola estampa en cada verificación y en cada registro. */
  get id(): number | null {
    return this.leer()?.id ?? null;
  }

  private leer(): PorteriaGuardada | null {
    const crudo = localStorage.getItem(LLAVE);
    if (!crudo) {
      return null;
    }
    try {
      const dato = JSON.parse(crudo) as PorteriaGuardada;
      // Se valida la forma: un storage corrupto no puede tumbar la garita.
      return typeof dato?.id === 'number' ? dato : null;
    } catch {
      localStorage.removeItem(LLAVE);
      return null;
    }
  }
}
