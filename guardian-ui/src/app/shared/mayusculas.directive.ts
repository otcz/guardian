import { Directive, ElementRef, HostListener, Optional, Self } from '@angular/core';
import { NgControl } from '@angular/forms';

/**
 * Convierte a mayúsculas lo que se escribe, se pega o se autocompleta en un
 * campo — nombres, documentos, placas, direcciones.
 *
 * <p>Cubre `formControlName` y `[(ngModel)]` por igual: los dos implementan
 * `NgControl`, y esta directiva escribe a través de él para que el control
 * quede en mayúsculas, no solo lo que se ve en pantalla.</p>
 *
 * <p>Reescribe el `<input>` completo en cada evento, así que el cursor
 * saltaría solo al final si no se restaura a mano — quien corrige una letra
 * en medio del nombre lo notaría de inmediato.</p>
 *
 * <p>NO va en contraseñas, PIN, correo ni en el campo único de la garita: el
 * primero porque cambiaría lo que la persona en verdad tecleó, el correo
 * porque el backend lo guarda en minúsculas como llave de recuperación, y la
 * garita porque ese campo también recibe el payload firmado del QR —
 * `GRD1.<uuid>.<firma>`—, que es sensible a mayúsculas y dejaría de validar.</p>
 */
@Directive({
  selector: '[gdMayusculas]'
})
export class MayusculasDirective {

  constructor(
    private readonly elemento: ElementRef<HTMLInputElement | HTMLTextAreaElement>,
    @Optional() @Self() private readonly control: NgControl | null
  ) {}

  @HostListener('input', ['$event.target'])
  alEscribir(campo: HTMLInputElement | HTMLTextAreaElement): void {
    const mayusculas = campo.value.toUpperCase();
    if (mayusculas === campo.value) {
      return;
    }

    const inicio = campo.selectionStart;
    const fin = campo.selectionEnd;

    if (this.control?.control) {
      this.control.control.setValue(mayusculas);
    } else {
      campo.value = mayusculas;
    }

    // La conversión no cambia el largo del texto — toUpperCase() es
    // carácter a carácter—, así que la misma posición sigue siendo válida.
    // Se aplica en el siguiente turno porque Angular todavía no repintó el
    // valor nuevo en el DOM cuando este evento termina.
    queueMicrotask(() => campo.setSelectionRange(inicio, fin));
  }
}
