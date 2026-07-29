import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ResidenteLayoutComponent } from './layout/residente-layout/residente-layout.component';
import { PorteriaLayoutComponent } from './layout/porteria-layout/porteria-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { invitadoGuard, rolGuard, sesionGuard } from './core/guards/sesion.guard';

/**
 * Tres paneles, tres layouts. Cada rol aterriza en el suyo y los cruces son
 * atajos explícitos en el encabezado, no un menú común revuelto.
 */
const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./modules/auth/auth.module').then(m => m.AuthModule),
    canActivate: [invitadoGuard]
  },
  {
    // Fuera de todo layout: es la única pantalla permitida con el cambio de
    // clave pendiente.
    path: 'cambiar-clave',
    loadChildren: () =>
      import('./modules/auth/cambio-clave.module').then(m => m.CambioClaveModule)
  },

  // ── Panel del usuario ────────────────────────────────────────────────────
  {
    path: 'app',
    component: ResidenteLayoutComponent,
    canActivate: [sesionGuard],
    children: [
      { path: '', redirectTo: 'mi-qr', pathMatch: 'full' },
      {
        path: 'mi-qr',
        loadChildren: () =>
          import('./modules/residente/residente.module').then(m => m.ResidenteModule)
      },
      {
        path: 'mi-hogar',
        loadChildren: () =>
          import('./modules/mi-hogar/mi-hogar.module').then(m => m.MiHogarModule)
      },
      {
        path: 'invitados',
        loadChildren: () =>
          import('./modules/invitados/invitados.module').then(m => m.InvitadosModule)
      }
    ]
  },

  // ── Pagina publica del invitado (link compartido, sin sesion) ────────────
  {
    path: 'invitado',
    loadChildren: () =>
      import('./modules/invitado-publico/invitado-publico.module')
        .then(m => m.InvitadoPublicoModule)
  },

  // ── Consola de portería ──────────────────────────────────────────────────
  {
    path: 'porteria',
    component: PorteriaLayoutComponent,
    canActivate: [rolGuard('GUARDIA', 'ADMIN')],
    children: [
      {
        path: '',
        loadChildren: () => import('./modules/garita/garita.module').then(m => m.GaritaModule)
      }
    ]
  },

  // ── Back-office de administración ────────────────────────────────────────
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [rolGuard('ADMIN')],
    children: [
      {
        path: '',
        loadChildren: () => import('./modules/admin/admin.module').then(m => m.AdminModule)
      }
    ]
  },

  // Rutas históricas.
  { path: 'mi-qr', redirectTo: 'app/mi-qr' },
  { path: 'mi-hogar', redirectTo: 'app/mi-hogar' },
  { path: 'garita', redirectTo: 'porteria' },

  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
