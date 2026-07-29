import { Component, OnInit } from '@angular/core';

import { AdminService } from '../../../core/services/admin.service';
import { Resumen } from '../../../core/models/admin.model';
import { AccesoEvento } from '../../../core/models/acceso.model';

/**
 * Tablero de aterrizaje del back-office: presencia en vivo, inventario del
 * conjunto, actividad de hoy y los últimos movimientos.
 */
@Component({
  selector: 'gd-admin-resumen',
  templateUrl: './resumen.component.html',
  styleUrl: './resumen.component.scss',
  standalone: false
})
export class ResumenComponent implements OnInit {

  resumen: Resumen | null = null;
  cargando = true;
  error: string | null = null;

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;

    this.admin.resumen().subscribe({
      next: resumen => {
        this.resumen = resumen;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar el resumen.';
        this.cargando = false;
      }
    });
  }

  permitido(evento: AccesoEvento): boolean {
    return evento.resultado === 'PERMITIDO';
  }
}
