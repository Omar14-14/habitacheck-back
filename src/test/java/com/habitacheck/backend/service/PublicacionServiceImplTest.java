package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.CrearPublicacionRequest;
import com.habitacheck.backend.entity.*;
import com.habitacheck.backend.exception.PublicacionInvalidaException;
import com.habitacheck.backend.repository.*;
import com.habitacheck.backend.service.impl.PublicacionServiceImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceImplTest {
    @Mock UsuarioRepository usuarios;
    @Mock UsuarioRolRepository roles;
    @Mock ServicioRepository servicios;
    @Mock PropiedadRepository propiedades;
    PublicacionService publicaciones;
    Usuario usuario;

    @BeforeEach
    void preparar() {
        publicaciones = new PublicacionServiceImpl(usuarios, roles, servicios, propiedades);
        usuario = new Usuario("Ana", "ana@example.com", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(usuario, "idUsuario", 7L);
        when(usuarios.findByCorreoIgnoreCase("ana@example.com")).thenReturn(Optional.of(usuario));
    }

    @Test
    void creaPublicacionDelArrendadorConFotosOrdenadasYServicios() {
        when(roles.existsByUsuario_IdUsuarioAndRol_Nombre(7L, "arrendador")).thenReturn(true);
        var agua = new Servicio("Agua", null, "activo");
        ReflectionTestUtils.setField(agua, "idServicio", 2L);
        when(servicios.findAllById(any())).thenReturn(List.of(agua));
        var respuesta = publicaciones.crear("ana@example.com", request(List.of(2L)));
        var captor = ArgumentCaptor.forClass(Propiedad.class);
        verify(propiedades).saveAndFlush(captor.capture());
        assertSame(usuario, captor.getValue().getArrendador());
        assertEquals("disponible", respuesta.estado());
        assertEquals(7L, respuesta.idArrendador());
        assertEquals("Casa", respuesta.titulo());
        assertEquals("S/N", respuesta.numeroExterior());
        assertNull(respuesta.numeroInterior());
        assertEquals(List.of((short) 1, (short) 2), respuesta.fotografias().stream()
                .map(foto -> foto.orden()).toList());
        assertEquals("https://example.com/portada.jpg", respuesta.fotografias().get(0).url());
        assertEquals(2L, respuesta.servicios().get(0).idServicio());
    }

    @Test
    void arrendatarioNoGuardaNada() {
        assertThrows(AccessDeniedException.class,
                () -> publicaciones.crear("ana@example.com", request(List.of())));
        verifyNoInteractions(servicios, propiedades);
    }

    @Test
    void usuarioSuspendidoNoPuedePublicarAunqueTengaRol() {
        ReflectionTestUtils.setField(usuario, "estado", "suspendido");
        assertThrows(AccessDeniedException.class,
                () -> publicaciones.crear("ana@example.com", request(List.of())));
        verifyNoInteractions(roles, servicios, propiedades);
    }

    @Test
    void rechazaServiciosInexistentes() {
        when(roles.existsByUsuario_IdUsuarioAndRol_Nombre(7L, "arrendador")).thenReturn(true);
        when(servicios.findAllById(any())).thenReturn(List.of());
        assertThrows(PublicacionInvalidaException.class,
                () -> publicaciones.crear("ana@example.com", request(List.of(99L))));
        verifyNoInteractions(propiedades);
    }

    @Test
    void rechazaServiciosInactivos() {
        when(roles.existsByUsuario_IdUsuarioAndRol_Nombre(7L, "arrendador")).thenReturn(true);
        when(servicios.findAllById(any())).thenReturn(List.of(new Servicio("Agua", null, "inactivo")));
        assertThrows(PublicacionInvalidaException.class,
                () -> publicaciones.crear("ana@example.com", request(List.of(2L))));
        verifyNoInteractions(propiedades);
    }

    @Test
    void rechazaServiciosDuplicadosAntesDeConsultarCatalogo() {
        when(roles.existsByUsuario_IdUsuarioAndRol_Nombre(7L, "arrendador")).thenReturn(true);
        assertThrows(PublicacionInvalidaException.class,
                () -> publicaciones.crear("ana@example.com", request(List.of(2L, 2L))));
        verifyNoInteractions(servicios, propiedades);
    }

    private CrearPublicacionRequest request(List<Long> ids) {
        return new CrearPublicacionRequest(" Casa ", "Descripción", new BigDecimal("5500.00"),
                "Juárez", "S/N", " ", "Centro", "Puebla", "Puebla", "72000",
                List.of("https://example.com/portada.jpg", "https://example.com/interior.jpg"), ids);
    }
}
