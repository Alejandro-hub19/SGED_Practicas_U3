package org.uteq.backend.auth.dto;

import lombok.Builder;

/**
 * Respuesta del login.
 *
 * IMPORTANTE: ya NO contiene los campos 'token' ni 'refreshToken'. Los tokens
 * viajan unicamente en cookies HttpOnly (ver CookieUtils y ADR-004). Exponerlos
 * aqui obligaria al frontend a guardarlos en localStorage, que es legible por
 * cualquier script inyectado (XSS).
 */
@Builder
public record LoginResponse(
        String username,
        String nombre,
        String rol
) {}
