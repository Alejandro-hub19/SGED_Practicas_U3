package org.uteq.backend.auth.service;

import org.uteq.backend.auth.dto.LoginResponse;

/**
 * Objeto interno de transporte entre AuthService y AuthController.
 *
 * Separa deliberadamente los tokens (que van a la cookie HttpOnly) del perfil
 * (que si viaja en el cuerpo JSON). De este modo es imposible, por descuido,
 * serializar el JWT en la respuesta.
 */
public record TokenBundle(
        String accessToken,
        String refreshToken,
        LoginResponse perfil
) {}
