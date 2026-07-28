import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { MiHogarComponent } from './mi-hogar/mi-hogar.component';

const routes: Routes = [{ path: '', component: MiHogarComponent }];

@NgModule({
  declarations: [MiHogarComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TooltipModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class MiHogarModule { }
