package com.habitacheck.backend;

import com.habitacheck.backend.repository.UsuarioRolRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "HABITACHECK_INTEGRATION_TESTS", matches = "true")
class RegistroIntegrationTest {

    private static final String CORREO = "registro.qa@example.com";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean UsuarioRolRepository asignaciones;

    @BeforeEach
    void comprobarBaseAislada() {
        assertEquals("habitacheck_registro_test",
                jdbc.queryForObject("SELECT current_database()", String.class),
                "Estas pruebas solo se ejecutan en habitacheck_registro_test");
        limpiar();
    }

    @AfterEach
    void limpiar() {
        // No ejecutar limpieza si la configuración apunta a otra base de datos.
        if (!"habitacheck_registro_test".equals(
                jdbc.queryForObject("SELECT current_database()", String.class))) {
            return;
        }
        jdbc.update("DELETE FROM usuario_rol WHERE id_usuario IN "
                + "(SELECT id_usuario FROM usuarios WHERE lower(correo) = ?)", CORREO);
        jdbc.update("DELETE FROM usuarios WHERE lower(correo) = ?", CORREO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"arrendatario", "arrendador"})
    void registraUsuarioConHashYRelacionReal(String rol) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CORREO, rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value(rol))
                .andExpect(jsonPath("$.password").doesNotExist());

        String hash = jdbc.queryForObject("SELECT password FROM usuarios WHERE correo = ?",
                String.class, CORREO);
        assertNotEquals("ClaveSegura123!", hash);
        assertTrue(encoder.matches("ClaveSegura123!", hash));
        assertEquals(rol, jdbc.queryForObject("""
                SELECT r.nombre FROM usuario_rol ur
                JOIN roles r ON r.id_rol = ur.id_rol
                JOIN usuarios u ON u.id_usuario = ur.id_usuario
                WHERE u.correo = ?
                """, String.class, CORREO));
    }

    @Test
    void correoDuplicadoIgnoraMayusculasYEspacios() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CORREO, "arrendatario")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body(" REGISTRO.QA@EXAMPLE.COM ", "arrendador")))
                .andExpect(status().isConflict());
        assertEquals(1L, cantidadUsuarios());
    }

    @Test
    void fallaAsignacionYSeDeshaceElUsuarioReal() throws Exception {
        doThrow(new DataIntegrityViolationException("Fallo simulado de la asignación"))
                .when(asignaciones).saveAndFlush(any());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CORREO, "arrendatario")))
                .andExpect(status().isInternalServerError());

        assertEquals(0L, cantidadUsuarios());
    }

    @Test
    void dosSolicitudesConcurrentesCreanSoloUnUsuario() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var primera = executor.submit(() -> enviarAlIniciar(inicio));
            var segunda = executor.submit(() -> enviarAlIniciar(inicio));
            inicio.countDown();
            var estados = List.of(primera.get(30, TimeUnit.SECONDS),
                    segunda.get(30, TimeUnit.SECONDS)).stream().sorted().toList();
            assertEquals(List.of(201, 409), estados);
            assertEquals(1L, cantidadUsuarios());
        } finally {
            executor.shutdownNow();
        }
    }

    private int enviarAlIniciar(CountDownLatch inicio) throws Exception {
        assertTrue(inicio.await(10, TimeUnit.SECONDS));
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body(CORREO, "arrendatario")))
                .andReturn().getResponse().getStatus();
    }

    private long cantidadUsuarios() {
        return jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE lower(correo) = ?",
                Long.class, CORREO);
    }

    private String body(String correo, String rol) {
        return """
                {"nombre":"Ana","correo":"%s","password":"ClaveSegura123!","rol":"%s"}
                """.formatted(correo, rol);
    }
}
