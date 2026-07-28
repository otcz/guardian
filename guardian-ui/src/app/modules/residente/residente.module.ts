import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { QRCodeModule } from 'angularx-qrcode';

import { MiQrComponent } from './mi-qr/mi-qr.component';

const routes: Routes = [{ path: '', component: MiQrComponent }];

@NgModule({
  declarations: [MiQrComponent],
  imports: [CommonModule, QRCodeModule, RouterModule.forChild(routes)]
})
export class ResidenteModule { }
