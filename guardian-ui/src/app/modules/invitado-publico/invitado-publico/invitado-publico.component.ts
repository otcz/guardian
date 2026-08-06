import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { InvitacionPublicaService } from '../../../core/services/invitacion-publica.service';
import { InvitacionPublica } from '../../../core/models/acceso.model';

@Component({
  selector: 'gd-invitado-publico',
  templateUrl: './invitado-publico.component.html',
  styleUrl: './invitado-publico.component.scss',
  standalone: false
})
export class InvitadoPublicoComponent implements OnInit {

  invitacion: InvitacionPublica | null = null;
  cargando = true;
  error = false;

  constructor(
    private readonly ruta: ActivatedRoute,
    private readonly servicio: InvitacionPublicaService
  ) {}

  ngOnInit(): void {
    const codigo = this.ruta.snapshot.paramMap.get('codigo') ?? '';

    this.servicio.ver(codigo).subscribe({
      next: invitacion => {
        this.invitacion = invitacion;
        this.cargando = false;
      },
      error: () => {
        this.error = true;
        this.cargando = false;
      }
    });
  }

  get utilizable(): boolean {
    const estado = this.invitacion?.estado;
    return estado === 'VIGENTE' || estado === 'NO_VIGENTE';
  }

  get textoEstado(): string {
    switch (this.invitacion?.estado) {
      case 'NO_VIGENTE': return 'Tu invitación aún no está vigente.';
      // Solo lo ven invitaciones viejas: las nuevas no tienen tope de entradas.
      case 'AGOTADA': return 'Esta invitación ya usó todos sus ingresos.';
      case 'VENCIDA': return 'Esta invitación ya venció.';
      case 'REVOCADA': return 'Esta invitación fue cancelada.';
      default: return '';
    }
  }
}
