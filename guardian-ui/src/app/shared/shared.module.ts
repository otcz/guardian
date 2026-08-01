import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';

import { FotoUploadComponent } from './foto-upload/foto-upload.component';
import { FotoPipe } from './foto-upload/foto.pipe';
import { DocumentoPipe } from './documento.pipe';
import { HojaComponent } from './hoja/hoja.component';

@NgModule({
  declarations: [FotoUploadComponent, FotoPipe, DocumentoPipe, HojaComponent],
  imports: [CommonModule, TooltipModule],
  exports: [FotoUploadComponent, FotoPipe, DocumentoPipe, HojaComponent]
})
export class SharedModule { }
