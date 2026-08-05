import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { ResumenComponent } from './resumen/resumen.component';
import { CasasComponent } from './casas/casas.component';
import { PorteriasComponent } from './porterias/porterias.component';
import { SolicitudesComponent } from './solicitudes/solicitudes.component';
import { PersonasComponent } from './personas/personas.component';
import { VehiculosComponent } from './vehiculos/vehiculos.component';
import { InvitacionesComponent } from './invitaciones/invitaciones.component';
import { BitacoraComponent } from './bitacora/bitacora.component';
import { ConfiguracionComponent } from './configuracion/configuracion.component';

// El marco (sidebar) lo pone AdminLayoutComponent a nivel de aplicación;
// este módulo solo aporta las pantallas.
const routes: Routes = [
  { path: '', redirectTo: 'resumen', pathMatch: 'full' },
  { path: 'resumen', component: ResumenComponent },
  { path: 'casas', component: CasasComponent },
  { path: 'personas', component: PersonasComponent },
  { path: 'vehiculos', component: VehiculosComponent },
  // Ruta histórica: el acceso se administra en Personas desde que las dos
  // pantallas se fundieron. Redirige para no romper enlaces guardados.
  { path: 'usuarios', redirectTo: 'personas' },
  { path: 'invitaciones', component: InvitacionesComponent },
  { path: 'bitacora', component: BitacoraComponent },
  { path: 'solicitudes', component: SolicitudesComponent },
  { path: 'porterias', component: PorteriasComponent },
  { path: 'configuracion', component: ConfiguracionComponent }
];

@NgModule({
  declarations: [
    ResumenComponent,
    CasasComponent,
    PorteriasComponent,
    SolicitudesComponent,
    PersonasComponent,
    VehiculosComponent,
    InvitacionesComponent,
    BitacoraComponent,
    ConfiguracionComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TooltipModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class AdminModule { }
