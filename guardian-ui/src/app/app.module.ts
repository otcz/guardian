import { NgModule, isDevMode } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ServiceWorkerModule } from '@angular/service-worker';
import { providePrimeNG } from 'primeng/config';
import { TooltipModule } from 'primeng/tooltip';
import Aura from '@primeng/themes/aura';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ShellComponent } from './layout/shell/shell.component';

@NgModule({
  declarations: [
    AppComponent,
    ShellComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    TooltipModule,
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
          // <html>, igual que los tokens de app/styles/_tokens.scss. Si se
          // dejara el default 'system', PrimeNG y los tokens podrian quedar en
          // modos distintos y la interfaz saldria a mitad de camino.
          darkModeSelector: '.dark-mode'
        }
      }
    }),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
