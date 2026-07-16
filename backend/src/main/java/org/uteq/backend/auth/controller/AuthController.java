package org.uteq.backend.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.dto.LoginResponse;
import org.uteq.backend.auth.dto.RegistroRequest;
import org.uteq.backend.auth.security.CookieUtils;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.service.AuthService;
import org.uteq.backend.auth.service.TokenBundle;

import java.util.Map;

/**
 * Endpoints de autenticacion.
 *
 * Contrato de seguridad:
 *  - POST /api/auth/login   -> Set-Cookie: SGED_ACCESS (HttpOnly) + SGED_REFRESH
 *                              El cuerpo devuelve SOLO datos de perfil, nunca el token.
 *  - POST /api/auth/logout  -> revoca el JTI en Redis y borra ambas cookies.
 *  - GET  /api/auth/me      -> requiere token valido y NO revocado.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * En localhost (HTTP) debe ser false o el navegador descarta la cookie.
     * En produccion (HTTPS) se pone en true via variable de entorno.
     */
    @Value("${security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${security.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @GetMapping("/ping")
    public String ping() {
        return "AUTH OK";
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody @Valid RegistroRequest request) {
        return ResponseEntity.status(201).body(authService.registro(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {

        TokenBundle bundle = authService.login(request);

        ResponseCookie access = CookieUtils.crearAccessCookie(
                bundle.accessToken(), jwtService.getExpirationMs(), cookieSecure);

        ResponseCookie refresh = CookieUtils.crearRefreshCookie(
                bundle.refreshToken(), refreshExpirationMs, cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .header(HttpHeaders.SET_COOKIE, refresh.toString())
                .body(bundle.perfil());   // sin token en el cuerpo
    }

    /**
     * Logout: toma el token de la cookie (o de la cabecera, como respaldo),
     * registra su JTI en la blacklist de Redis con TTL = vida restante del
     * token, y ordena al navegador borrar las cookies.
     *
     * A partir de aqui, cualquier peticion con ese mismo token sera rechazada
     * por el JwtAuthenticationFilter.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {

        authService.logout(request);

        ResponseCookie borrarAccess  = CookieUtils.borrar(CookieUtils.ACCESS_COOKIE, cookieSecure);
        ResponseCookie borrarRefresh = CookieUtils.borrar(CookieUtils.REFRESH_COOKIE, cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, borrarAccess.toString())
                .header(HttpHeaders.SET_COOKIE, borrarRefresh.toString())
                .body(Map.of("mensaje", "Sesion cerrada correctamente"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {

        TokenBundle bundle = authService.refresh(request);

        ResponseCookie access = CookieUtils.crearAccessCookie(
                bundle.accessToken(), jwtService.getExpirationMs(), cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, access.toString())
                .body(bundle.perfil());
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.obtenerPerfil(authentication.getName()));
    }
}
