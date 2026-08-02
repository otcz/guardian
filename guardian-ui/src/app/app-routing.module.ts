import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ResidenteLayoutComponent } from './layout/residente-layout/residente-layout.component';
import { PorteriaLayoutComponent } from './layout/porteria-layout/porteria-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import {
  invitadoGuard,
  panelAdminGuard,
  rolGuard,
  sesionGuard
} from './core/guards/sesion.guard';

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
  {
    // "Olvidé mi contraseña". Sin guard de invitado a propósito: quien tiene
    // una sesión vieja en el teléfono y olvidó la clave igual debe poder
    // recuperarla sin que lo reboten a un panel al que no puede volver a
    // entrar cuando la sesión expire.
    path: 'recuperar',
    loadChildren: () =>
      import('./modules/recuperar/recuperar.module').then(m => m.RecuperarModule)
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

  // ── Registro en un hogar por codigo (sin sesion) ─────────────────────────
  {
    // Quien llega aquí todavía no existe en el sistema: sale con cuenta
    // propia. El código de un solo uso es la única llave.
    path: 'unirme',
    loadChildren: () =>
      import('./modules/unirme/unirme.module').then(m => m.UnirmeModule)
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

  // ── Panel de la plataforma (multisede) ───────────────────────────────────
  {
    // Sin layout propio: es UNA pantalla, y la barra lateral de secciones del
    // back-office no aplica acá — desde la plataforma no se gestionan casas
    // ni personas, se entra a una sede para hacerlo.
    path: 'sedes',
    canActivate: [rolGuard('SUPER_ADMIN')],
    loadChildren: () => import('./modules/super/super.module').then(m => m.SuperModule)
  },

  // ── Back-office de administración ────────────────────────────────────────
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [panelAdminGuard],
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
