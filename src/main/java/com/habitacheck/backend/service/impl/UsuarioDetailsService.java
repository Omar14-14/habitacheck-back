package com.habitacheck.backend.service.impl;

import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final UsuarioRolRepository roles;

    public UsuarioDetailsService(UsuarioRepository usuarios, UsuarioRolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        var usuario = usuarios.findByCorreoIgnoreCase(correo.strip())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        String[] autoridades = roles.findByUsuario_IdUsuario(usuario.getIdUsuario()).stream()
                .map(asignacion -> asignacion.getRol().getNombre().toUpperCase(Locale.ROOT))
                .toArray(String[]::new);
        return User.withUsername(usuario.getCorreo()).password(usuario.getPassword())
                .roles(autoridades).disabled(!"activo".equals(usuario.getEstado())).build();
    }
}
