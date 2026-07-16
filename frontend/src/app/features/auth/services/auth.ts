import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../../environments/environments';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  username: string;
  nombre: string;
  rol: string;
}

/**
 * El JWT viaja unicamente en una cookie HttpOnly (ver ADR-004): el backend ya
 * no lo devuelve en el cuerpo de la respuesta ni este servicio lo guarda en
 * localStorage, porque un script inyectado por XSS podria leerlo desde ahi.
 *
 * Consecuencia directa: este servicio no puede saber por si mismo si hay
 * sesion activa (no tiene forma de leer la cookie). La unica fuente de verdad
 * es preguntarle al servidor via GET /api/auth/me, que es lo que hace
 * checkSession(). Los guards de rutas llaman a checkSession() antes de activar
 * cualquier ruta protegida.
 */
@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  readonly currentUser = signal<LoginResponse | null>(null);

  login(username: string, password: string): Observable<LoginResponse> {
    const body: LoginRequest = { username, password };
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/auth/login`, body, { withCredentials: true })
      .pipe(tap((res) => this.currentUser.set(res)));
  }

  /**
   * Cierra sesion en el servidor: registra el JTI del token en la blacklist de
   * Redis para que quede invalidado de inmediato, no solo hasta su expiracion
   * natural. Si la llamada de red falla igual se limpia el estado local, para
   * no dejar al usuario atascado en una pantalla protegida.
   */
  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true })
      .pipe(
        tap(() => this.currentUser.set(null)),
        catchError(() => {
          this.currentUser.set(null);
          return of(void 0);
        })
      );
  }

  /**
   * Pregunta al backend si la cookie vigente sigue siendo valida y no
   * revocada, y refresca el perfil en memoria. Devuelve false ante cualquier
   * error (401 esperado si no hay sesion, o la cookie fue revocada).
   */
  checkSession(): Observable<boolean> {
    return this.http
      .get<LoginResponse>(`${this.apiUrl}/auth/me`, { withCredentials: true })
      .pipe(
        tap((res) => this.currentUser.set(res)),
        map(() => true),
        catchError(() => {
          this.currentUser.set(null);
          return of(false);
        })
      );
  }

  getRol(): string | null {
    return this.currentUser()?.rol ?? null;
  }

  getUser(): LoginResponse | null {
    return this.currentUser();
  }
}
