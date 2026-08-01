import { Injectable } from '@angular/core';

const LLAVE = 'guardian.tema';
const ID_META_TEMA = 'gd-theme-color';

/** Copia literal de --bg-base en cada modo (app/styles/_tokens.scss). */
const FONDO_CLARO = '#f4f6f8';
const FONDO_OSCURO = '#0e1420';

/**
 * Tema claro/oscuro compartido por los tres paneles.
 *
 * <p>Por defecto <b>sigue al sistema</b>: quien tiene el teléfono en oscuro no
 * debería abrir GUARDIAN en blanco y tener que buscar un botón de luna. Elegir
 * a mano guarda la preferencia por dispositivo, que es lo que necesita la
 * tablet de la portería: se deja en oscuro una vez y queda.</p>
 */
@Injectable({ providedIn: 'root' })
export class TemaService {

  private readonly consultaSistema = window.matchMedia('(prefers-color-scheme: dark)');

  constructor() {
    // Si el sistema cambia de tema (atardecer, modo automático) y el usuario
    // no eligió nada, la app cambia con él sin recargar.
    this.consultaSistema.addEventListener('change', () => {
      if (!this.hayPreferenciaGuardada) {
        this.aplicar();
      }
    });
    this.aplicar();
  }

  get oscuro(): boolean {
    return document.documentElement.classList.contains('dark-mode');
  }

  alternar(): void {
    localStorage.setItem(LLAVE, this.oscuro ? 'claro' : 'oscuro');
    this.aplicar();
  }

  /** Vuelve a seguir al sistema. */
  seguirAlSistema(): void {
    localStorage.removeItem(LLAVE);
    this.aplicar();
  }

  private get hayPreferenciaGuardada(): boolean {
    const guardada = localStorage.getItem(LLAVE);
    return guardada === 'claro' || guardada === 'oscuro';
  }

  private aplicar(): void {
    const guardada = localStorage.getItem(LLAVE);
    const oscuro = this.hayPreferenciaGuardada
      ? guardada === 'oscuro'
      : this.consultaSistema.matches;

    document.documentElement.classList.toggle('dark-mode', oscuro);
    this.sincronizarBarraDeEstado(oscuro);
  }

  /**
   * Los meta theme-color del index responden a prefers-color-scheme, que NO
   * cambia al alternar el tema a mano. Este meta sin media gana sobre ambos y
   * mantiene la barra de estado del mismo color que la pantalla.
   */
  private sincronizarBarraDeEstado(oscuro: boolean): void {
    let meta = document.getElementById(ID_META_TEMA) as HTMLMetaElement | null;

    if (!meta) {
      meta = document.createElement('meta');
      meta.id = ID_META_TEMA;
      meta.name = 'theme-color';
      document.head.appendChild(meta);
    }
    meta.content = oscuro ? FONDO_OSCURO : FONDO_CLARO;
  }
}
