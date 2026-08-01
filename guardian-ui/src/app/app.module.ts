import { LOCALE_ID, NgModule, isDevMode } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCO from '@angular/common/locales/es-CO';
import { BrowserModule } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ServiceWorkerModule } from '@angular/service-worker';
import { providePrimeNG } from 'primeng/config';
import { TooltipModule } from 'primeng/tooltip';
import Aura from '@primeng/themes/aura';

import { AppRoutingModule } from './app-routing.module';
import { SharedModule } from './shared/shared.module';
import { AppComponent } from './app.component';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ResidenteLayoutComponent } from './layout/residente-layout/residente-layout.component';
import { PorteriaLayoutComponent } from './layout/porteria-layout/porteria-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';

// Sin esto Angular formatea con en-US y una app en español muestra "1 Aug"
// y "8/1/2026" — el detalle que delata que la interfaz no se cuidó.
registerLocaleData(localeEsCO);

@NgModule({
  declarations: [
    AppComponent,
    ResidenteLayoutComponent,
    PorteriaLayoutComponent,
    AdminLayoutComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    TooltipModule,
    // Los tres layouts se declaran acá, y el del residente usa gd-hoja.
    SharedModule,
    ServiceWorkerModule.register('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000'
    })
  ],
  providers: [
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          // GUARDIAN maneja su propio modo oscuro con la clase .dark-mode en
          // <html> (TemaService). Si se dejara el default 'system', PrimeNG y
          // los tokens podrian quedar en modos distintos.
          darkModeSelector: '.dark-mode'
        }
      }
    }),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: LOCALE_ID, useValue: 'es-CO' }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
