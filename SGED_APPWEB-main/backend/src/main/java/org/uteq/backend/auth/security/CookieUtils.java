package org.uteq.backend.auth.security;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Construccion centralizada de las cookies HttpOnly que transportan el JWT.
 *
 * Decision de diseno (ver ADR-004): el token de acceso NUNCA se devuelve en el
 * cuerpo de la respuesta ni se guarda en localStorage. Viaja exclusivamente en
 * una cookie con los flags HttpOnly, Secure y SameSite, de modo que el
 * JavaScript del navegador no puede leerlo. Esto mitiga el robo de token por
 * XSS (OWASP A03: Injection / A07: Authentication Failures).
 */
public final class CookieUtils {

    public static final String ACCESS_COOKIE = "SGED_ACCESS";
    public static final String REFRESH_COOKIE = "SGED_REFRESH";

    private CookieUtils() {
        // clase de utilidad: no instanciable
    }

    /**
     * Crea la cookie que contiene el token de acceso.
     *
     * @param token     JWT firmado
     * @param maxAgeMs  vigencia en milisegundos (debe coincidir con la
     *                  expiracion del propio JWT)
     * @param secure    true en produccion (HTTPS). En localhost debe ir en
     *                  false, porque el navegador descarta cookies Secure
     *                  enviadas por HTTP.
     */
    public static ResponseCookie crearAccessCookie(String token, long maxAgeMs, boolean secure) {
        return construir(ACCESS_COOKIE, token, maxAgeMs, secure);
    }

    public static ResponseCookie crearRefreshCookie(String token, long maxAgeMs, boolean secure) {
        return construir(REFRESH_COOKIE, token, maxAgeMs, secure);
    }

    /**
     * Cookie de borrado: mismo nombre, valor vacio y Max-Age = 0. El navegador
     * la elimina inmediatamente. Se usa en el logout.
     */
    public static ResponseCookie borrar(String nombre, boolean secure) {
        return ResponseCookie.from(nombre, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private static ResponseCookie construir(String nombre, String valor, long maxAgeMs, boolean secure) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)                       // inaccesible desde JavaScript
                .secure(secure)                       // solo por HTTPS en produccion
                .sameSite("Lax")                      // mitiga CSRF en navegacion cross-site
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }
}
