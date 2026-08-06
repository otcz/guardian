import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { QRCodeModule } from 'angularx-qrcode';

import { InvitadoPublicoComponent } from './invitado-publico/invitado-publico.component';
import { SharedModule } from '../../shared/shared.module';

const routes: Routes = [{ path: ':codigo', component: InvitadoPublicoComponent }];

/**
 * La pagina que abre el INVITADO desde el link que le compartieron. Vive fuera
 * de todos los layouts y no exige sesion: el codigo UUID es la llave.
 */
@NgModule({
  declarations: [InvitadoPublicoComponent],
  imports: [CommonModule, SharedModule, QRCodeModule, RouterModule.forChild(routes)]
})
export class InvitadoPublicoModule { }
