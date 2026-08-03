import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Cuatro dígitos. Espejo de `Codigos.PIN_LONGITUD` en el backend. */
export const PIN_LONGITUD = 4;

/**
 * Los mismos PIN que rechaza `PinUtil` en el backend.
 *
 * <p>Duplicar la lista es deliberado, y el backend sigue siendo la autoridad:
 * esto solo evita que la persona escriba 1234, toque guardar y reciba un error
 * del servidor tres segundos después. Si las dos listas se desalinean, el peor
 * caso es que un PIN pase aquí y el servidor lo rechace — nunca al revés.</p>
 */
const TRIVIALES = construirTriviales();

function construirTriviales(): ReadonlySet<string> {
  const prohibidos = new Set<string>();

  for (let digito = 0; digito <= 9; digito++) {
    let repetido = '';
    let sube = '';
    let baja = '';
    for (let i = 0; i < PIN_LONGITUD; i++) {
      repetido += digito;
      sube += (digito + i) % 10;
      baja += ((digito - i) % 10 + 10) % 10;
    }
    prohibidos.add(repetido).add(sube).add(baja);
  }

  ['2580', '0852', '1379', '9731',
   '1990', '1991', '1995', '1998', '1999', '2000', '2001', '2020']
    .forEach(p => prohibidos.add(p));

  return prohibidos;
}

/**
 * Valida forma y trivialidad en un solo control.
 *
 * <p>Devuelve claves de error distintas —`pinForma` y `pinTrivial`— para que la
 * pantalla pueda decir QUÉ corregir. Un "PIN inválido" a secas manda a la
 * persona a probar otro trivial hasta que acierte uno permitido.</p>
 */
export function validadorPin(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = control.value;
    if (!valor) {
      return null; // De la obligatoriedad se encarga Validators.required.
    }
    if (!/^\d{4}$/.test(valor)) {
      return { pinForma: true };
    }
    if (TRIVIALES.has(valor)) {
      return { pinTrivial: true };
    }
    return null;
  };
}
