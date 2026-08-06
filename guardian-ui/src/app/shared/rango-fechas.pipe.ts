import { Pipe, PipeTransform } from '@angular/core';

/**
 * La ventana de una visita en una línea, con su hora:
 *
 * <ul>
 *   <li>mismo día → `5 ago · 19:00 – 23:59`</li>
 *   <li>varios días → `5 ago 19:00 – 7 ago 23:59`</li>
 * </ul>
 *
 * <p>Vive acá porque lo pintan tres pantallas —las invitaciones del residente,
 * la página del invitado y la tabla del administrador— y escrito tres veces se
 * corrige en una y se olvida en las otras.</p>
 *
 * <p>Hora en 24 horas: es más corta que "7:00 p. m.", no se confunde de mitad
 * del día y es como se escriben los turnos de la portería.</p>
 *
 * <p>El año solo aparece cuando NO es el actual: en una lista de visitas de
 * esta semana repetirlo en cada renglón es ruido, y en una invitación vieja su
 * ausencia haría creer que es de este año.</p>
 */
@Pipe({ name: 'rangoFechas', standalone: false })
export class RangoFechasPipe implements PipeTransform {

  private static readonly DIA_MES =
    new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short' });

  private static readonly DIA_MES_ANIO =
    new Intl.DateTimeFormat('es', { day: 'numeric', month: 'short', year: 'numeric' });

  private static readonly HORA =
    new Intl.DateTimeFormat('es', { hour: '2-digit', minute: '2-digit', hour12: false });

  transform(desde: string | Date | null | undefined,
            hasta: string | Date | null | undefined): string {
    if (!desde || !hasta) {
      return '';
    }
    const inicio = new Date(desde);
    const fin = new Date(hasta);
    if (isNaN(inicio.getTime()) || isNaN(fin.getTime())) {
      return '';
    }

    const esteAnio = new Date().getFullYear();
    const conAnio = inicio.getFullYear() !== esteAnio || fin.getFullYear() !== esteAnio;
    const dia = conAnio ? RangoFechasPipe.DIA_MES_ANIO : RangoFechasPipe.DIA_MES;
    const hora = RangoFechasPipe.HORA;

    // El mismo día se escribe una sola vez: repetirlo a lado y lado es ruido
    // justo donde se lee de un vistazo.
    if (inicio.toDateString() === fin.toDateString()) {
      return `${dia.format(fin)} · ${hora.format(inicio)} – ${hora.format(fin)}`;
    }
    return `${dia.format(inicio)} ${hora.format(inicio)} – `
      + `${dia.format(fin)} ${hora.format(fin)}`;
  }
}
