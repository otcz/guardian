import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { EscanerComponent } from './escaner/escaner.component';

const routes: Routes = [{ path: '', component: EscanerComponent }];

@NgModule({
  declarations: [EscanerComponent],
  imports: [CommonModule, FormsModule, TooltipModule, RouterModule.forChild(routes)]
})
export class GaritaModule { }
