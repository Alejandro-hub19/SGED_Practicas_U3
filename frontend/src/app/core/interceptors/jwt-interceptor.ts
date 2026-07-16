import { HttpInterceptorFn } from '@angular/common/http';

/**
 * El JWT vive en una cookie HttpOnly (ver ADR-004): este interceptor ya no
 * puede leerlo ni adjuntarlo como header Authorization. Su unico trabajo es
 * garantizar que el navegador envie esa cookie en cada peticion, incluida la
 * de login/logout, que es cross-origin (Angular en :4200, API en :8080) y por
 * defecto el navegador NO manda cookies en peticiones cross-origin sin
 * withCredentials.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
