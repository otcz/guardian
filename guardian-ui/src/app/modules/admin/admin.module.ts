import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { AdminShellComponent } from './admin-shell/admin-shell.component';
import { CasasComponent } from './casas/casas.component';
import { PersonasComponent } from './personas/personas.component';
import { VehiculosComponent } from './vehiculos/vehiculos.component';
import { UsuariosComponent } from './usuarios/usuarios.component';
import { BitacoraComponent } from './bitacora/bitacora.component';

const routes: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'casas', pathMatch: 'full' },
      { path: 'casas', component: CasasComponent },
      { path: 'personas', component: PersonasComponent },
      { path: 'vehiculos', component: VehiculosComponent },
      { path: 'usuarios', component: UsuariosComponent },
      { path: 'bitacora', component: BitacoraComponent }
    ]
  }
];

@NgModule({
  declarations: [
    AdminShellComponent,
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
