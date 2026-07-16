package org.uteq.backend.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.dto.LoginResponse;
import org.uteq.backend.auth.dto.RegistroRequest;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.entity.Rol;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.auth.repository.RolRepository;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.repository.UsuarioRolRepository;
import org.uteq.backend.auth.security.CookieUtils;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.security.RedisBlacklistService;
import org.uteq.backend.common.exception.ConflictoException;
import org.uteq.backend.common.exception.CredencialesInvalidasException;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisBlacklistService blacklistService;

    @Transactional
    public Usuario registro(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new ConflictoException("El username ya existe");
        }

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .activo(true)
                .build();
        personaRepository.save(persona);

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .activo(true)
                .build();
        usuarioRepository.save(usuario);

        Rol rol = rolRepository.findByNombre("USER")
                .orElseGet(() -> rolRepository.save(
                        new Rol(null, "USER", "Usuario estandar")
                ));

        usuarioRolRepository.save(new UsuarioRol(null, usuario, rol));

        return usuario;
    }

    @Transactional(readOnly = true)
    public TokenBundle login(LoginRequest request) {

        Usuario usuario = usuarioRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "Usuario o contrasena incorrectos"));

        if (usuario.getActivo() == null || !usuario.getActivo()) {
            throw new CredencialesInvalidasException("El usuario esta inactivo");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos");
        }

        List<UsuarioRol> roles = usuarioRolRepository.findByUsuario(usuario);
        String rol = roles.isEmpty() ? "USER" : roles.get(0).getRol().getNombre();

        String accessToken  = jwtService.generateToken(usuario.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(usuario.getUsername(), rol);

        LoginResponse perfil = LoginResponse.builder()
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();

        return new TokenBundle(accessToken, refreshToken, perfil);
    }

    @Transactional(readOnly = true)
    public TokenBundle refresh(HttpServletRequest request) {

        String refreshToken = leerCookie(request, CookieUtils.REFRESH_COOKIE)
                .orElseThrow(() -> new CredencialesInvalidasException(
                        "No se encontro el token de refresco"));

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new CredencialesInvalidasException("Token de refresco invalido o expirado");
        }

        // Un refresh token revocado tampoco debe servir.
        String jtiRefresh = jwtService.extractJti(refreshToken);
        if (jtiRefresh != null && blacklistService.estaRevocado(jtiRefresh)) {
            throw new CredencialesInvalidasException("Token de refresco revocado");
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String nuevoAccess = jwtService.generateToken(usuario.getUsername(), rol);

        LoginResponse perfil = LoginResponse.builder()
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();

        return new TokenBundle(nuevoAccess, refreshToken, perfil);
    }

    /**
     * Perfil del usuario autenticado, para GET /api/auth/me.
     *
     * El frontend no puede leer el JWT (viaja en cookie HttpOnly), asi que esta
     * es la unica forma de que Angular sepa si hay sesion activa y con que rol,
     * despues de un login o de recargar la pagina.
     */
    @Transactional(readOnly = true)
    public LoginResponse obtenerPerfil(String username) {

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        List<UsuarioRol> roles = usuarioRolRepository.findByUsuario(usuario);
        String rol = roles.isEmpty() ? "USER" : roles.get(0).getRol().getNombre();

        return LoginResponse.builder()
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();
    }

    /**
     * Revoca el token actual insertando su JTI en Redis.
     *
     * El TTL se calcula como la vida RESTANTE del token, no como su duracion
     * total: una entrada que sobreviva a la expiracion natural del JWT no
     * aporta seguridad y solo ocupa memoria. Redis elimina la clave sola.
     */
    public void logout(HttpServletRequest request) {

        Optional<String> tokenOpt = leerCookie(request, CookieUtils.ACCESS_COOKIE);

        if (tokenOpt.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                tokenOpt = Optional.of(authHeader.substring(7));
            }
        }

        tokenOpt.ifPresent(token -> {
            try {
                String jti = jwtService.extractJti(token);
                long ttl = jwtService.getTiempoRestanteMs(token);
                if (jti != null && ttl > 0) {
                    blacklistService.revocarToken(jti, ttl);
                }
                // El refresh token, si viene, tambien se revoca.
                leerCookie(request, CookieUtils.REFRESH_COOKIE).ifPresent(rt -> {
                    String jtiRt = jwtService.extractJti(rt);
                    long ttlRt = jwtService.getTiempoRestanteMs(rt);
                    if (jtiRt != null && ttlRt > 0) {
                        blacklistService.revocarToken(jtiRt, ttlRt);
                    }
                });
            } catch (Exception ignored) {
                // Token ya expirado o malformado: no hay nada que revocar.
            }
        });
    }

    private Optional<String> leerCookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> nombre.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
