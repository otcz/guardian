import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { CasasComponent } from './casas/casas.component';
import { PersonasComponent } from './personas/personas.component';

const routes: Routes = [
  { path: '', redirectTo: 'casas', pathMatch: 'full' },
  { path: 'casas', component: CasasComponent },
  { path: 'personas', component: PersonasComponent }
];

@NgModule({
  declarations: [CasasComponent, PersonasComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TooltipModule,
    RouterModule.forChild(routes)
  ]
})
export class AdminModule { }
