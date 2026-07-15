package org.uteq.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.uteq.backend.auth.security.CookieUtils;
import org.uteq.backend.auth.security.CustomUserDetailsService;
import org.uteq.backend.auth.security.JwtAuthenticationFilter;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.security.RedisBlacklistService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Prueba el nucleo del requisito (2a): que el logout invalide efectivamente el
 * token.
 *
 * No levanta el contexto de Spring ni necesita Redis real: se comprueba el
 * CONTRATO del filtro. Un mismo token, valido y bien firmado, debe autenticar
 * cuando su JTI no esta en la blacklist y NO debe autenticar cuando si lo esta.
 *
 * La evidencia end-to-end (login -> 200, logout -> 401) se obtiene con
 * scripts/demo-logout.ps1 contra la aplicacion en ejecucion.
 */
@ExtendWith(MockitoExtension.class)
class JwtBlacklistFilterTest {

    private static final String TOKEN = "token.firmado.valido";
    private static final String JTI   = "6f1c9b2e-0000-4a11-9f3c-aaaabbbbcccc";

    @Mock  private JwtService jwtService;
    @Mock  private CustomUserDetailsService userDetailsService;
    @Mock  private RedisBlacklistService blacklistService;
    @Mock  private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filtro;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Token valido y NO revocado -> la peticion queda autenticada")
    void tokenVigenteAutentica() throws Exception {

        UserDetails usuario = User.withUsername("dpallop")
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ADMINISTRADOR")))
                .build();

        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractJti(TOKEN)).thenReturn(JTI);
        when(blacklistService.estaRevocado(JTI)).thenReturn(false);
        when(jwtService.extractUsername(TOKEN)).thenReturn("dpallop");
        when(userDetailsService.loadUserByUsername("dpallop")).thenReturn(usuario);

        filtro.doFilter(peticionConCookie(), new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("un token vigente debe poblar el SecurityContext")
                .isNotNull();

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token valido pero REVOCADO en Redis -> NO autentica (logout efectivo)")
    void tokenRevocadoNoAutentica() throws Exception {

        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractJti(TOKEN)).thenReturn(JTI);
        when(blacklistService.estaRevocado(JTI)).thenReturn(true);   // <- logout previo

        filtro.doFilter(peticionConCookie(), new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("un token revocado NO debe autenticar: el logout lo invalida")
                .isNull();

        // Nunca debe llegar a cargar el usuario: se corta antes.
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(any(), any());
    }

    private MockHttpServletRequest peticionConCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/estudiantes");
        request.setCookies(new Cookie(CookieUtils.ACCESS_COOKIE, TOKEN));
        return request;
    }
}
