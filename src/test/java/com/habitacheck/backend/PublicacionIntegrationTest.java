package com.habitacheck.backend;

import com.habitacheck.backend.repository.PropiedadRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "HABITACHECK_INTEGRATION_TESTS", matches = "true")
class PublicacionIntegrationTest {
    private static final String CORREO = "publicaciones.qa@example.com";
    private static final String PASSWORD = "ClaveSegura123!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean PropiedadRepository propiedades;
    Long servicioId;

    @BeforeEach
    void prepararBaseAislada() throws Exception {
        assertEquals("habitacheck_registro_test",
                jdbc.queryForObject("SELECT current_database()", String.class));
        limpiar();
        servicioId = jdbc.queryForObject("""
                INSERT INTO servicios (nombre, estado) VALUES ('Agua QA publicaciones', 'activo')
                RETURNING id_servicio
                """, Long.class);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Ana QA","correo":"publicaciones.qa@example.com",
                 "password":"ClaveSegura123!","rol":"arrendador"}
                """))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void limpiar() {
        if (!"habitacheck_registro_test".equals(jdbc.queryForObject("SELECT current_database()", String.class))) {
            return;
        }
        String filtro = "SELECT p.id_propiedad FROM propiedades p JOIN usuarios u "
                + "ON u.id_usuario = p.id_arrendador WHERE u.correo = ?";
        jdbc.update("DELETE FROM propiedad_servicio WHERE id_propiedad IN (" + filtro + ")", CORREO);
        jdbc.update("DELETE FROM propiedad_fotografia WHERE id_propiedad IN (" + filtro + ")", CORREO);
        jdbc.update("DELETE FROM propiedades WHERE id_arrendador IN "
                + "(SELECT id_usuario FROM usuarios WHERE correo = ?)", CORREO);
        jdbc.update("DELETE FROM usuario_rol WHERE id_usuario IN "
                + "(SELECT id_usuario FROM usuarios WHERE correo = ?)", CORREO);
        jdbc.update("DELETE FROM usuarios WHERE correo = ?", CORREO);
        if (servicioId != null) {
            jdbc.update("DELETE FROM servicios WHERE id_servicio = ?", servicioId);
        }
    }

    @Test
    void autenticaConCuentaRealYGuardaPublicacionFotosYServicios() throws Exception {
        publicar().andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("disponible"))
                .andExpect(jsonPath("$.fotografias[0].orden").value(1))
                .andExpect(jsonPath("$.fotografias[1].orden").value(2))
                .andExpect(jsonPath("$.fotografias[0].idFotografia").isNumber())
                .andExpect(jsonPath("$.servicios[0].idServicio").value(servicioId))
                .andExpect(jsonPath("$.password").doesNotExist());
        assertEquals(1L, cantidadPropiedades());
        assertEquals(List.of(1, 2), jdbc.queryForList("""
                SELECT f.orden FROM propiedad_fotografia f JOIN propiedades p USING (id_propiedad)
                JOIN usuarios u ON u.id_usuario = p.id_arrendador WHERE u.correo = ? ORDER BY f.orden
                """, Integer.class, CORREO));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM propiedad_servicio WHERE id_servicio = ?", Long.class, servicioId));
    }

    @Test
    void falloDespuesDeInsertarRevierteTodoElAgregado() throws Exception {
        doAnswer(invocacion -> {
            invocacion.callRealMethod();
            throw new DataIntegrityViolationException("Fallo de persistencia simulado después del flush");
        }).when(propiedades).saveAndFlush(any());
        publicar().andExpect(status().isInternalServerError());
        assertEquals(0L, cantidadPropiedades());
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM propiedad_servicio WHERE id_servicio = ?", Long.class, servicioId));
    }

    @Test
    void passwordIncorrectaYCuentaSuspendidaNoAutentican() throws Exception {
        mvc.perform(get("/api/auth/csrf").with(httpBasic(CORREO, "incorrecta")))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE usuarios SET estado = 'suspendido' WHERE correo = ?", CORREO);
        mvc.perform(get("/api/auth/csrf").with(httpBasic(CORREO, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions publicar() throws Exception {
        var token = mvc.perform(get("/api/auth/csrf").with(httpBasic(CORREO, PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        String csrf = JsonPath.read(token.getResponse().getContentAsString(), "$.token");
        String header = JsonPath.read(token.getResponse().getContentAsString(), "$.headerName");
        return mvc.perform(post("/api/publicaciones").with(httpBasic(CORREO, PASSWORD))
                .session((MockHttpSession) token.getRequest().getSession(false))
                .header(header, csrf).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Casa QA","descripcion":"Publicación de prueba","precioMensual":5500.00,
                 "calle":"Juárez","numeroExterior":"S/N","colonia":"Centro","municipio":"Puebla",
                 "estadoUbicacion":"Puebla","codigoPostal":"72000",
                 "fotografias":["https://example.com/1.jpg","https://example.com/2.jpg"],
                 "servicios":[%d]}
                """.formatted(servicioId)));
    }

    private long cantidadPropiedades() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM propiedades p JOIN usuarios u ON u.id_usuario = p.id_arrendador
                WHERE u.correo = ?
                """, Long.class, CORREO);
    }
}
