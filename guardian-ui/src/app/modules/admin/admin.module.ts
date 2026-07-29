import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { ResumenComponent } from './resumen/resumen.component';
import { CasasComponent } from './casas/casas.component';
import { PersonasComponent } from './personas/personas.component';
import { VehiculosComponent } from './vehiculos/vehiculos.component';
import { UsuariosComponent } from './usuarios/usuarios.component';
import { BitacoraComponent } from './bitacora/bitacora.component';

// El marco (sidebar) lo pone AdminLayoutComponent a nivel de aplicación;
// este módulo solo aporta las pantallas.
const routes: Routes = [
  { path: '', redirectTo: 'resumen', pathMatch: 'full' },
  { path: 'resumen', component: ResumenComponent },
  { path: 'casas', component: CasasComponent },
  { path: 'personas', component: PersonasComponent },
  { path: 'vehiculos', component: VehiculosComponent },
  { path: 'usuarios', component: UsuariosComponent },
  { path: 'bitacora', component: BitacoraComponent }
];

@NgModule({
  declarations: [
    ResumenComponent,
    CasasComponent,
    PersonasComponent,
    VehiculosComponent,
    UsuariosComponent,
    BitacoraComponent
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
