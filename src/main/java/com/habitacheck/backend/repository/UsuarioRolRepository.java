package com.habitacheck.backend.repository;

import com.habitacheck.backend.entity.Usuario;
import com.habitacheck.backend.entity.UsuarioRol;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    List<UsuarioRol> findByUsuario(Usuario usuario);
}
