import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Captura del motivo de un bloqueo administrativo.
 *
 * <p>El motivo es obligatorio a propósito: un bloqueo sin explicación deja al
 * siguiente administrador — y al guardia que tiene que dar la cara en la
 * portería — sin saber por qué esa persona no entra.</p>
 *
 * <p>Se apoya en <code>gd-hoja</code> en vez de dibujar otro diálogo: así
 * hereda el atrapado de foco, el Escape y el bloqueo de scroll ya resueltos.</p>
 */
@Component({
  selector: 'gd-bloqueo',
  templateUrl: './bloqueo.component.html',
  styleUrl: './bloqueo.component.scss',
  standalone: false
})
export class BloqueoComponent {

  /** Qué se está bloqueando: "Ana Díaz", "El vehículo ABC123". */
  @Input() objetivo = '';

  /** Consecuencia concreta. Vacío = la del ingreso al conjunto. */
  @Input() descripcion = '';

  @Output() confirmar = new EventEmitter<string>();
  @Output() cerrar = new EventEmitter<void>();

  motivo = '';

  /**
   * El motivo se limpia al abrir, no al cerrar: si quedara del bloqueo
   * anterior, el siguiente se registraría con una razón que no es la suya.
   */
  @Input()
  set abierta(valor: boolean) {
    if (valor && !this.visible) {
      this.motivo = '';
    }
    this.visible = valor;
  }

  visible = false;

  get texto(): string {
    return this.descripcion
      || `${this.objetivo} no podrá ingresar hasta que la administración levante `
         + 'el bloqueo. El titular no puede quitarlo desde su celular.';
  }

  aceptar(): void {
    const motivo = this.motivo.trim();
    if (motivo) {
      this.confirmar.emit(motivo);
    }
  }
}
