package com.habitacheck.backend.controller;

import com.habitacheck.backend.config.SecurityConfig;
import com.habitacheck.backend.dto.request.CrearPublicacionRequest;
import com.habitacheck.backend.exception.GlobalExceptionHandler;
import com.habitacheck.backend.exception.PublicacionInvalidaException;
import com.habitacheck.backend.service.PublicacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PublicacionController.class, CsrfController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicacionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PublicacionService publicaciones;

    @Test
    void utilizaIdentidadAutenticadaAunqueElJsonIntenteAsignarOtroPropietario() throws Exception {
        mvc.perform(post("/api/publicaciones").with(user("ana@example.com").roles("ARRENDADOR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body().replace("\"titulo\":", "\"idArrendador\":999,\"titulo\":")))
                .andExpect(status().isCreated());
        var captor = ArgumentCaptor.forClass(CrearPublicacionRequest.class);
        verify(publicaciones).crear(eq("ana@example.com"), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Casa", captor.getValue().titulo());
    }

    @Test
    void anonimoNoPuedePublicar() throws Exception {
        mvc.perform(post("/api/publicaciones").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(publicaciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARRENDATARIO", "ADMINISTRADOR"})
    void otrosRolesNoPuedenPublicar(String rol) throws Exception {
        mvc.perform(post("/api/publicaciones").with(user("otro").roles(rol)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(publicaciones);
    }

    @Test
    void exigeCsrf() throws Exception {
        mvc.perform(post("/api/publicaciones").with(user("ana").roles("ARRENDADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(publicaciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"codigoPostal\":\"72000\"|\"codigoPostal\":\"1234\"",
            "\"precioMensual\":5500.00|\"precioMensual\":0",
            "\"precioMensual\":5500.00|\"precioMensual\":1.234",
            "\"precioMensual\":5500.00|\"precioMensual\":10000000000",
            "\"titulo\":\" Casa \"|\"titulo\":\"   \"",
            "\"fotografias\":[]|\"fotografias\":[\"javascript:alert(1)\"]",
            "\"fotografias\":[]|\"fotografias\":[null]",
            "\"servicios\":[]|\"servicios\":[-1]",
            "\"servicios\":[]|\"servicios\":[null]"
    })
    void rechazaDatosInvalidosAntesDeInvocarServicio(String cambio) throws Exception {
        var partes = cambio.split("\\|", 2);
        mvc.perform(post("/api/publicaciones").with(user("ana").roles("ARRENDADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().replace(partes[0], partes[1])))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(publicaciones);
    }

    @Test
    void errorDeCatalogoDevuelve400() throws Exception {
        when(publicaciones.crear(anyString(), any()))
                .thenThrow(new PublicacionInvalidaException("Servicio inactivo"));
        mvc.perform(post("/api/publicaciones").with(user("ana").roles("ARRENDADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void autorizacionDelServicioDevuelve403() throws Exception {
        when(publicaciones.crear(anyString(), any())).thenThrow(new AccessDeniedException("Cuenta inactiva"));
        mvc.perform(post("/api/publicaciones").with(user("ana").roles("ARRENDADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    void entregaTokenCsrfAlClienteAutenticado() throws Exception {
        mvc.perform(get("/api/auth/csrf").with(user("ana").roles("ARRENDADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private String body() {
        return """
                {"titulo":" Casa ","descripcion":"Descripción","precioMensual":5500.00,
                 "calle":"Juárez","numeroExterior":"S/N","colonia":"Centro",
                 "municipio":"Puebla","estadoUbicacion":"Puebla","codigoPostal":"72000",
                 "fotografias":[],"servicios":[]}
                """;
    }
}
