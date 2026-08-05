import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { MiHogarComponent } from './mi-hogar/mi-hogar.component';

const routes: Routes = [{ path: '', component: MiHogarComponent }];

@NgModule({
  declarations: [MiHogarComponent],
  imports: [
    CommonModule,
    // La elección de casa usa [(ngModel)]: son dos selects sueltos, no un
    // formulario reactivo como el alta de familiares.
    FormsModule,
    ReactiveFormsModule,
    TooltipModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class MiHogarModule { }
