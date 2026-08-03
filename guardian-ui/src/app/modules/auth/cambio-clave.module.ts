import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';

import { SharedModule } from '../../shared/shared.module';
import { CambiarClaveComponent } from './cambiar-clave/cambiar-clave.component';

const routes: Routes = [{ path: '', component: CambiarClaveComponent }];

/**
 * Módulo aparte del de login porque la pantalla de cambio de clave vive fuera
 * del shell y con sus propias reglas de acceso: se llega a ella con sesión
 * abierta pero sin permiso de entrar a ninguna otra parte.
 */
@NgModule({
  declarations: [CambiarClaveComponent],
  imports: [CommonModule, ReactiveFormsModule, TooltipModule, SharedModule,
            RouterModule.forChild(routes)]
})
export class CambioClaveModule { }
