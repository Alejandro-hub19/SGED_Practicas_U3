package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.repository.UsuarioRolRepository;

import java.util.List;

/**
 * CORRECCION DE DEFECTO (documentar en el informe):
 *
 * La version anterior otorgaba la autoridad con prefijo, "ROLE_ADMINISTRADOR",
 * mientras que los controladores la comprueban con hasAnyAuthority('ADMINISTRADOR').
 * hasAnyAuthority compara la cadena EXACTA y no anade el prefijo ROLE_ (a
 * diferencia de hasRole). El resultado era que TODOS los endpoints protegidos
 * respondian 403, incluso con un token valido.
 *
 * Se unifica el criterio: la autoridad se emite SIN prefijo y todas las
 * comprobaciones usan hasAuthority / hasAnyAuthority.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<UsuarioRol> roles = usuarioRolRepository.findByUsuarioConRol(usuario);

        List<SimpleGrantedAuthority> authorities = roles.isEmpty()
                ? List.of(new SimpleGrantedAuthority("USER"))
                : roles.stream()
                       .map(ur -> new SimpleGrantedAuthority(ur.getRol().getNombre()))
                       .toList();

        boolean activo = usuario.getActivo() != null && usuario.getActivo();

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(!activo)
                .build();
    }
}
