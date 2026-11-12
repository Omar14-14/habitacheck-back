package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.ActualizarPerfilRequest;
import com.habitacheck.backend.entity.Rol;
import com.habitacheck.backend.entity.Usuario;
import com.habitacheck.backend.entity.UsuarioRol;
import com.habitacheck.backend.exception.UsuarioNoEncontradoException;
import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import com.habitacheck.backend.service.impl.PerfilServiceImpl;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilServiceImplTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioRolRepository usuarioRolRepository;

    private PerfilService service;

    private Usuario usuarioFijo;
    private UsuarioRol usuarioRolFijo;

    @BeforeEach
    void preparar() {
        service = new PerfilServiceImpl(usuarioRepository, usuarioRolRepository);

        usuarioFijo = new Usuario("Ana López", "ana@example.com",
                "{bcrypt}hash", "5512345678",
                OffsetDateTime.now(ZoneOffset.UTC));

        Rol rol = new Rol("arrendatario", "Rol de prueba");
        usuarioRolFijo = new UsuarioRol(usuarioFijo, rol, OffsetDateTime.now(ZoneOffset.UTC));
    }

    // ── obtenerPerfil ─────────────────────────────────────────────────────────

    @Test
    void obtenerPerfilDevuelveTodosLosCampos() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioFijo));
        when(usuarioRolRepository.findByUsuario(usuarioFijo)).thenReturn(List.of(usuarioRolFijo));

        var respuesta = service.obtenerPerfil(1L);

        assertEquals("Ana López", respuesta.nombre());
        assertEquals("ana@example.com", respuesta.correo());
        assertEquals("5512345678", respuesta.telefono());
        assertNull(respuesta.fotoPerfil());
        assertEquals(List.of("arrendatario"), respuesta.roles());
        assertEquals("activo", respuesta.estado());
    }

    @Test
    void obtenerPerfilLanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> service.obtenerPerfil(99L));
    }

    // ── actualizarPerfil ──────────────────────────────────────────────────────

    @Test
    void actualizarNombreYTelefono() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioFijo));
        when(usuarioRolRepository.findByUsuario(usuarioFijo)).thenReturn(List.of(usuarioRolFijo));
        when(usuarioRepository.saveAndFlush(usuarioFijo)).thenReturn(usuarioFijo);

        var request = new ActualizarPerfilRequest("María García", "5599887766");
        var respuesta = service.actualizarPerfil(1L, request);

        assertEquals("María García", respuesta.nombre());
        assertEquals("5599887766", respuesta.telefono());
        verify(usuarioRepository).saveAndFlush(usuarioFijo);
    }

    @Test
    void actualizarSoloNombreMantieneTelefono() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioFijo));
        when(usuarioRolRepository.findByUsuario(usuarioFijo)).thenReturn(List.of(usuarioRolFijo));
        when(usuarioRepository.saveAndFlush(usuarioFijo)).thenReturn(usuarioFijo);

        var request = new ActualizarPerfilRequest("Nuevo Nombre", null);
        var respuesta = service.actualizarPerfil(1L, request);

        assertEquals("Nuevo Nombre", respuesta.nombre());
        // teléfono no se pasa → null → se borra; comportamiento esperado según la spec
        verify(usuarioRepository).saveAndFlush(usuarioFijo);
    }

    // ── actualizarFoto ────────────────────────────────────────────────────────

    @Test
    void subirFotoGuardaDataUri() {
        byte[] bytesImagen = new byte[]{(byte) 0xFF, (byte) 0xD8}; // cabecera JPEG mínima
        MockMultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", bytesImagen);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioFijo));
        when(usuarioRolRepository.findByUsuario(usuarioFijo)).thenReturn(List.of(usuarioRolFijo));
        when(usuarioRepository.saveAndFlush(usuarioFijo)).thenReturn(usuarioFijo);

        var respuesta = service.actualizarFoto(1L, foto);

        assertNotNull(respuesta.fotoPerfil());
        assertTrue(respuesta.fotoPerfil().startsWith("data:image/jpeg;base64,"));
        verify(usuarioRepository).saveAndFlush(usuarioFijo);
    }

    @Test
    void rechazaArchivoNoImagen() {
        MockMultipartFile pdf = new MockMultipartFile(
                "foto", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarFoto(1L, pdf));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaFotoVacia() {
        MockMultipartFile vacia = new MockMultipartFile(
                "foto", "vacia.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarFoto(1L, vacia));
        verifyNoInteractions(usuarioRepository);
    }

    // ── eliminarFoto ──────────────────────────────────────────────────────────

    @Test
    void eliminarFotoPoneNullEnFotoPerfil() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioFijo));
        when(usuarioRolRepository.findByUsuario(usuarioFijo)).thenReturn(List.of(usuarioRolFijo));
        when(usuarioRepository.saveAndFlush(usuarioFijo)).thenReturn(usuarioFijo);

        var respuesta = service.eliminarFoto(1L);

        assertNull(respuesta.fotoPerfil());
        verify(usuarioRepository).saveAndFlush(usuarioFijo);
    }
}

