import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { QRCodeModule } from 'angularx-qrcode';

import { SharedModule } from '../../shared/shared.module';
import { MiQrComponent } from './mi-qr/mi-qr.component';

const routes: Routes = [{ path: '', component: MiQrComponent }];

@NgModule({
  declarations: [MiQrComponent],
  imports: [CommonModule, QRCodeModule, SharedModule, RouterModule.forChild(routes)]
})
export class ResidenteModule { }
