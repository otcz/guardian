import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { QRCodeModule } from 'angularx-qrcode';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { InvitadosComponent } from './invitados/invitados.component';

const routes: Routes = [{ path: '', component: InvitadosComponent }];

@NgModule({
  declarations: [InvitadosComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    QRCodeModule,
    TooltipModule,
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class InvitadosModule { }
