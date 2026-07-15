package org.uteq.backend.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuario(Usuario usuario);

    // Trae el rol en la misma consulta (evita el lazy-loading fuera de sesion)
    @Query("SELECT ur FROM UsuarioRol ur JOIN FETCH ur.rol WHERE ur.usuario = :usuario")
    List<UsuarioRol> findByUsuarioConRol(@Param("usuario") Usuario usuario);
}