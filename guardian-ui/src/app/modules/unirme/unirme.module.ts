import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { UnirmeComponent } from './unirme/unirme.component';

const routes: Routes = [{ path: ':codigo', component: UnirmeComponent }];

@NgModule({
  declarations: [UnirmeComponent],
  imports: [CommonModule, ReactiveFormsModule, SharedModule, RouterModule.forChild(routes)]
})
export class UnirmeModule { }
