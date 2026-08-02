import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';

import { FotoUploadComponent } from './foto-upload/foto-upload.component';
import { FotoPipe } from './foto-upload/foto.pipe';
import { DocumentoPipe } from './documento.pipe';
import { HojaComponent } from './hoja/hoja.component';
import { BloqueoComponent } from './bloqueo/bloqueo.component';
import { MotivoAccesoPipe } from './motivo-acceso.pipe';

@NgModule({
  declarations: [
    FotoUploadComponent,
    FotoPipe,
    DocumentoPipe,
    HojaComponent,
    BloqueoComponent,
    MotivoAccesoPipe
  ],
  imports: [CommonModule, FormsModule, TooltipModule],
  exports: [
    FotoUploadComponent,
    FotoPipe,
    DocumentoPipe,
    HojaComponent,
    BloqueoComponent,
    MotivoAccesoPipe
  ]
})
export class SharedModule { }
