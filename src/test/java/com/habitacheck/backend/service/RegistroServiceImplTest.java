package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.RegistroRequest;
import com.habitacheck.backend.dto.request.RolRegistro;
import com.habitacheck.backend.entity.Rol;
import com.habitacheck.backend.entity.Usuario;
import com.habitacheck.backend.entity.UsuarioRol;
import com.habitacheck.backend.exception.CorreoYaRegistradoException;
import com.habitacheck.backend.repository.RolRepository;
import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import com.habitacheck.backend.service.impl.RegistroServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroServiceImplTest {

    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;
    @Mock UsuarioRolRepository asignaciones;
    @Mock PasswordEncoder encoder;
    private RegistroService service;

    @BeforeEach
    void preparar() {
        service = new RegistroServiceImpl(usuarios, roles, asignaciones, encoder);
    }

    @ParameterizedTest
    @EnumSource(RolRegistro.class)
    void guardaHashYAsignaElRolElegido(RolRegistro elegido) {
        var request = new RegistroRequest(" Ana ", " ANA@EXAMPLE.COM ",
                "ClaveSegura123!", " 5512345678 ", elegido);
        Rol rol = new Rol(elegido.getNombre(), "Rol de prueba");
        when(roles.findByNombre(elegido.getNombre())).thenReturn(Optional.of(rol));
        when(encoder.encode(request.password())).thenReturn("{bcrypt}hash-de-prueba");

        var respuesta = service.registrar(request);

        var usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(usuario.capture());
        assertEquals("{bcrypt}hash-de-prueba", usuario.getValue().getPassword());
        assertEquals("ana@example.com", usuario.getValue().getCorreo());
        assertEquals("Ana", respuesta.nombre());
        assertEquals("activo", respuesta.estado());
        assertEquals(elegido.getNombre(), respuesta.roles().get(0));

        var asignacion = ArgumentCaptor.forClass(UsuarioRol.class);
        verify(asignaciones).saveAndFlush(asignacion.capture());
        assertSame(usuario.getValue(), asignacion.getValue().getUsuario());
        assertSame(rol, asignacion.getValue().getRol());
    }

    @Test
    void rechazaCorreoDuplicadoAntesDeGuardar() {
        var request = request();
        when(usuarios.existsByCorreoIgnoreCase(request.correo())).thenReturn(true);

        assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(request));

        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(roles, asignaciones, encoder);
    }

    @Test
    void noGuardaSiFaltaElRolEnElCatalogo() {
        when(roles.findByNombre("arrendatario")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.registrar(request()));

        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(asignaciones, encoder);
    }

    @Test
    void propagaElFalloDeAsignacionParaQueSpringDeshagaLaTransaccion() {
        when(roles.findByNombre("arrendatario"))
                .thenReturn(Optional.of(new Rol("arrendatario", "Prueba")));
        when(encoder.encode(any())).thenReturn("hash");
        when(asignaciones.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Fallo simulado"));

        assertThrows(DataIntegrityViolationException.class, () -> service.registrar(request()));
    }

    private RegistroRequest request() {
        return new RegistroRequest("Ana", "ana@example.com", "ClaveSegura123!",
                null, RolRegistro.ARRENDATARIO);
    }
}
