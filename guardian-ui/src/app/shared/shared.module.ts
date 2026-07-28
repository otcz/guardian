import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';

import { FotoUploadComponent } from './foto-upload/foto-upload.component';
import { FotoPipe } from './foto-upload/foto.pipe';

@NgModule({
  declarations: [FotoUploadComponent, FotoPipe],
  imports: [CommonModule, TooltipModule],
  exports: [FotoUploadComponent, FotoPipe]
})
export class SharedModule { }
