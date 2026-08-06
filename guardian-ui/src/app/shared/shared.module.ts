import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { FotoUploadComponent } from './foto-upload/foto-upload.component';
import { FotoPipe } from './foto-upload/foto.pipe';
import { DocumentoPipe } from './documento.pipe';
import { HojaComponent } from './hoja/hoja.component';
import { BloqueoComponent } from './bloqueo/bloqueo.component';
import { MotivoAccesoPipe } from './motivo-acceso.pipe';
import { RangoFechasPipe } from './rango-fechas.pipe';
import { FiltroColumnaComponent } from './tabla/filtro-columna.component';
import { VolverComponent } from './volver/volver.component';

@NgModule({
  declarations: [
    FotoUploadComponent,
    FotoPipe,
    DocumentoPipe,
    HojaComponent,
    BloqueoComponent,
    MotivoAccesoPipe,
    RangoFechasPipe,
    FiltroColumnaComponent,
    VolverComponent
  ],
  imports: [CommonModule, FormsModule, TooltipModule, RouterModule],
  exports: [
    FotoUploadComponent,
    FotoPipe,
    DocumentoPipe,
    HojaComponent,
    BloqueoComponent,
    MotivoAccesoPipe,
    RangoFechasPipe,
    FiltroColumnaComponent,
    VolverComponent
  ]
})
export class SharedModule { }
