import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Auth } from '../../features/auth/services/auth';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(Auth);
  const router = inject(Router);

  return auth.checkSession().pipe(
    map((autenticado) => {
      if (!autenticado) {
        return router.createUrlTree(['/login']);
      }

      const rolesPermitidos = route.data?.['roles'] as string[] | undefined;
      const rolActual = auth.getRol();

      if (!rolesPermitidos || rolesPermitidos.length === 0) {
        return true;
      }

      if (rolActual && rolesPermitidos.includes(rolActual)) {
        return true;
      }

      return router.createUrlTree(['/login']);
    })
  );
};
