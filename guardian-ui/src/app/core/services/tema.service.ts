import { Injectable } from '@angular/core';

const LLAVE = 'guardian.tema';

/**
 * Tema claro/oscuro compartido por los tres paneles. La preferencia se
 * recuerda por dispositivo: la tablet de la portería se deja en oscuro una
 * vez y queda.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {

  constructor() {
    if (localStorage.getItem(LLAVE) === 'oscuro') {
      document.documentElement.classList.add('dark-mode');
    }
  }

  get oscuro(): boolean {
    return document.documentElement.classList.contains('dark-mode');
  }

  alternar(): void {
    const activado = document.documentElement.classList.toggle('dark-mode');
    localStorage.setItem(LLAVE, activado ? 'oscuro' : 'claro');
  }
}
