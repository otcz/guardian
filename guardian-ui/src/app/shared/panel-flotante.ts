/** A qué borde del campo se pega el panel cuando es más ancho que él. */
export type Alineacion = 'izquierda' | 'derecha';

export interface Ubicacion {
  top: number;
  left: number;
}

/** Separación entre el campo y el panel, y respiro contra el borde de la pantalla. */
const AIRE = 6;
const MARGEN = 8;

/**
 * Dónde poner un panel que cuelga de un campo, en coordenadas de pantalla.
 *
 * <p>El panel se dibuja con `position: fixed` y no `absolute` a propósito: el
 * cuerpo de la hoja tiene su propio desplazamiento, y todo lo que se salga de
 * él lo recorta. Colgado del campo, un calendario que no cabía entre el campo
 * y el pie de la hoja aparecía cortado a la mitad, sin manera de ver el resto.
 * Con `fixed` el único límite es la pantalla — y de eso se encarga esta
 * función.</p>
 *
 * <p>Abre hacia abajo salvo que abajo no quepa y arriba sí. Si no cabe en
 * ninguno de los dos lados, se queda abajo y se pega al borde: mejor un panel
 * corrido que uno cortado.</p>
 */
export function ubicarPanel(
  campo: HTMLElement,
  panel: HTMLElement,
  alineacion: Alineacion = 'izquierda'
): Ubicacion {
  const caja = campo.getBoundingClientRect();
  const alto = panel.offsetHeight;
  const ancho = panel.offsetWidth;

  const cabeAbajo = window.innerHeight - caja.bottom - AIRE - MARGEN >= alto;
  const cabeArriba = caja.top - AIRE - MARGEN >= alto;

  const top = !cabeAbajo && cabeArriba
    ? caja.top - alto - AIRE
    : caja.bottom + AIRE;

  const left = alineacion === 'derecha' ? caja.right - ancho : caja.left;

  return {
    top: acotar(top, window.innerHeight - alto - MARGEN),
    left: acotar(left, window.innerWidth - ancho - MARGEN)
  };
}

/** Entre el margen y el tope. Si no cabe ni así, gana el margen de arriba. */
function acotar(valor: number, maximo: number): number {
  return Math.max(MARGEN, Math.min(valor, Math.max(MARGEN, maximo)));
}

/**
 * Conecta un panel abierto a los eventos que lo mueven, y devuelve cómo
 * desconectarlo.
 *
 * <p>El desplazamiento se sigue: el cuerpo de la hoja tiene el suyo y hay que
 * escucharlo en CAPTURA, porque el evento de scroll no burbujea.</p>
 *
 * <p>El cambio de tamaño NO se sigue, se cierra. Al girar el teléfono el
 * formulario entero se redistribuye, y recolocar el panel con la posición que
 * el campo tenía ANTES de esa redistribución lo dejaba fuera de la pantalla.
 * Cerrar es además lo que hace cualquier menú flotante cuando cambia la
 * ventana debajo.</p>
 */
export function seguirAlCampo(recolocar: () => void, cerrar: () => void): () => void {
  window.addEventListener('scroll', recolocar, true);
  window.addEventListener('resize', cerrar);
  return () => {
    window.removeEventListener('scroll', recolocar, true);
    window.removeEventListener('resize', cerrar);
  };
}
