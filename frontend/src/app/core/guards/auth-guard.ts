import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Auth } from '../../features/auth/services/auth';

/**
 * No hay token legible en el cliente (cookie HttpOnly), asi que la unica
 * forma de saber si hay sesion activa es preguntandole al servidor.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  const router = inject(Router);

  return auth
    .checkSession()
    .pipe(map((autenticado) => autenticado || router.createUrlTree(['/login'])));
};
