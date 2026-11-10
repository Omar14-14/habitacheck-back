package com.habitacheck.backend.service;

import com.habitacheck.backend.entity.*;
import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import com.habitacheck.backend.service.impl.UsuarioDetailsService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioDetailsServiceTest {
    @Test
    void cargaHashYRolesRealesYDeshabilitaCuentasSuspendidas() {
        var usuarios = mock(UsuarioRepository.class);
        var roles = mock(UsuarioRolRepository.class);
        var usuario = new Usuario("Ana", "ana@example.com", "{bcrypt}hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(usuario, "idUsuario", 7L);
        ReflectionTestUtils.setField(usuario, "estado", "suspendido");
        when(usuarios.findByCorreoIgnoreCase("ANA@example.com")).thenReturn(Optional.of(usuario));
        when(roles.findByUsuario_IdUsuario(7L)).thenReturn(List.of(new UsuarioRol(usuario,
                new Rol("arrendador", null), OffsetDateTime.now())));
        var detalles = new UsuarioDetailsService(usuarios, roles).loadUserByUsername(" ANA@example.com ");
        assertFalse(detalles.isEnabled());
        assertEquals("ana@example.com", detalles.getUsername());
        assertEquals("{bcrypt}hash", detalles.getPassword());
        assertTrue(detalles.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ARRENDADOR")));
    }

    @Test
    void cuentaInexistenteNoPuedeAutenticarse() {
        var usuarios = mock(UsuarioRepository.class);
        var roles = mock(UsuarioRolRepository.class);
        assertThrows(UsernameNotFoundException.class,
                () -> new UsuarioDetailsService(usuarios, roles).loadUserByUsername("nadie@example.com"));
    }
}
