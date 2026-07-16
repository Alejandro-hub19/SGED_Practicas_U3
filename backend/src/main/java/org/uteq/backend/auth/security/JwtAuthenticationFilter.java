package org.uteq.backend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filtro de autenticacion stateless.
 *
 * Orden de validacion en cada peticion:
 *   1. Extraer el JWT: primero de la cookie HttpOnly, y como respaldo de la
 *      cabecera Authorization (util para Postman y para las pruebas).
 *   2. Verificar firma y expiracion.
 *   3. Consultar la BLACKLIST en Redis por el JTI del token.
 *      Si el JTI esta revocado (logout previo), la peticion NO se autentica.
 *
 * El paso 3 es el que convierte el logout en una invalidacion real: sin el,
 * un token robado o "cerrado" seguiria siendo valido hasta su expiracion
 * natural, que es precisamente la debilidad conocida de JWT stateless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RedisBlacklistService blacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> tokenOpt = extraerToken(request);

        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = tokenOpt.get();

        try {
            // --- Validacion criptografica y de expiracion ---
            if (!jwtService.isTokenValid(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // --- Consulta a la blacklist de JTI en Redis ---
            String jti = jwtService.extractJti(jwt);
            if (jti != null && blacklistService.estaRevocado(jti)) {
                log.debug("Token rechazado: JTI {} se encuentra revocado en la blacklist", jti);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception ex) {
            // Token malformado, firma invalida o expirado: se deja pasar sin
            // autenticar y Spring Security respondera 401/403 mas adelante.
            log.debug("JWT no valido: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Prioridad: cookie HttpOnly > cabecera Authorization.
     * La cabecera se mantiene como respaldo para clientes no navegador
     * (Postman, scripts de benchmark, pruebas de integracion).
     */
    private Optional<String> extraerToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<String> desdeCookie = Arrays.stream(cookies)
                    .filter(c -> CookieUtils.ACCESS_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst();

            if (desdeCookie.isPresent()) {
                return desdeCookie;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }

        return Optional.empty();
    }
}
