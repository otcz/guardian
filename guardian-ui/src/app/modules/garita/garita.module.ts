import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { EscanerComponent } from './escaner/escaner.component';
import { HuellaComponent } from './huella/huella.component';

const routes: Routes = [{ path: '', component: EscanerComponent }];

@NgModule({
  declarations: [EscanerComponent, HuellaComponent],
  imports: [CommonModule, FormsModule, TooltipModule, SharedModule,
    RouterModule.forChild(routes)]
})
export class GaritaModule { }
