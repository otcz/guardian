import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Confirmación de una acción sin motivo que escribir — el reemplazo de
 * `window.confirm()`.
 *
 * <p>Se apoya en gd-hoja por la misma razón que gd-bloqueo: hereda el
 * atrapado de foco, el Escape y el bloqueo de scroll ya resueltos, en vez de
 * dejar que el navegador dibuje su propio diálogo gris con el dominio
 * encima — que no se puede estilizar y desentona con el resto de la app.</p>
 */
@Component({
  selector: 'gd-confirmar',
  templateUrl: './confirmar.component.html',
  styleUrl: './confirmar.component.scss',
  standalone: false
})
export class ConfirmarComponent {

  @Input() titulo = 'Confirmar';
  @Input() mensaje = '';
  @Input() textoConfirmar = 'Confirmar';

  /** Rojo cuando la acción quita algo; azul cuando es una vuelta atrás segura. */
  @Input() peligroso = false;

  @Input() abierta = false;

  @Output() confirmar = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();
}
