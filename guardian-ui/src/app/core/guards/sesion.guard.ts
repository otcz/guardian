import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { Rol } from '../models/sesion.model';

/** Exige sesión iniciada. */
export const sesionGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.autenticado) {
    return router.createUrlTree(['/ingreso']);
  }

  // El cambio de clave del primer ingreso se hace de verdad: mientras esté
  // pendiente, ninguna otra pantalla abre. Si no, la clave igual al documento
  // sobrevive indefinidamente y la cuenta queda expuesta.
  if (auth.requiereCambioClave) {
    return router.createUrlTree(['/cambiar-clave']);
  }

  return true;
};

/** Exige uno de los roles indicados. */
export const rolGuard = (...roles: Rol[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.autenticado) {
    return router.createUrlTree(['/ingreso']);
  }
  if (auth.requiereCambioClave) {
    return router.createUrlTree(['/cambiar-clave']);
  }
  if (!auth.tieneRol(...roles)) {
    return router.createUrlTree([auth.rutaInicial()]);
  }
  return true;
};

/**
 * Panel de administración: el ADMIN de la sede siempre, y el super
 * administrador solo DENTRO de una sede.
 *
 * Sin sede elegida su token no lleva conjunto, así que cada endpoint de
 * /api/admin le responde 403. Dejarlo entrar sería abrirle una pantalla que
 * solo puede mostrarle errores.
 */
export const panelAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.autenticado) {
    return router.createUrlTree(['/ingreso']);
  }
  if (auth.requiereCambioClave) {
    return router.createUrlTree(['/cambiar-clave']);
  }
  if (auth.tieneRol('ADMIN') || (auth.tieneRol('SUPER_ADMIN') && auth.sedeSuplantada)) {
    return true;
  }
  return router.createUrlTree([auth.rutaInicial()]);
};

/** Impide volver al login con sesión abierta. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.autenticado ? router.createUrlTree([auth.rutaInicial()]) : true;
};
