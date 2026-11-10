package com.habitacheck.backend.repository;

import com.habitacheck.backend.entity.UsuarioRol;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    boolean existsByUsuario_IdUsuarioAndRol_Nombre(Long idUsuario, String nombre);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "rol")
    List<UsuarioRol> findByUsuario_IdUsuario(Long idUsuario);
}
