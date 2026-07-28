import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ShellComponent } from './layout/shell/shell.component';
import { invitadoGuard, rolGuard, sesionGuard } from './core/guards/sesion.guard';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./modules/auth/auth.module').then(m => m.AuthModule),
    canActivate: [invitadoGuard]
  },
  {
    // El cambio de clave queda FUERA del shell y sin el guard de sesión: es la
    // única pantalla a la que se llega justamente por tener el cambio pendiente.
    path: 'cambiar-clave',
    loadChildren: () =>
      import('./modules/auth/cambio-clave.module').then(m => m.CambioClaveModule)
  },
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'mi-qr',
        loadChildren: () =>
          import('./modules/residente/residente.module').then(m => m.ResidenteModule),
        canActivate: [sesionGuard]
      },
      {
        path: 'garita',
        loadChildren: () => import('./modules/garita/garita.module').then(m => m.GaritaModule),
        canActivate: [rolGuard('GUARDIA', 'ADMIN')]
      },
      {
        path: 'admin',
        loadChildren: () => import('./modules/admin/admin.module').then(m => m.AdminModule),
        canActivate: [rolGuard('ADMIN')]
      }
    ]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
