package com.habitacheck.backend.controller;

import com.habitacheck.backend.config.SecurityConfig;
import com.habitacheck.backend.dto.response.RegistroResponse;
import com.habitacheck.backend.exception.CorreoYaRegistradoException;
import com.habitacheck.backend.exception.GlobalExceptionHandler;
import com.habitacheck.backend.service.RegistroService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean RegistroService service;

    @Test
    void registroPublicoDevuelve201SinPassword() throws Exception {
        when(service.registrar(any())).thenReturn(new RegistroResponse(
                1L, "Ana", "ana@example.com", null, "activo",
                List.of("arrendatario"), OffsetDateTime.now()));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana@example.com", "ClaveSegura123!", "arrendatario")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void correoInvalidoDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body("correo-invalido", "ClaveSegura123!", "arrendatario")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"administrador", "proveedor"})
    void rechazaRolesNoPermitidos(String rol) throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana@example.com", "ClaveSegura123!", rol)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void passwordUnicodeNoSuperaElLimiteDeBytesDeBcrypt() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana@example.com", "ñ".repeat(40), "arrendatario")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.password").exists());
        verifyNoInteractions(service);
    }

    @Test
    void correoDuplicadoDevuelve409() throws Exception {
        when(service.registrar(any())).thenThrow(new CorreoYaRegistradoException());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ana@example.com", "ClaveSegura123!", "arrendatario")))
                .andExpect(status().isConflict());
    }

    @Test
    void otrasRutasRequierenAutenticacion() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{correo:"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private String body(String correo, String password, String rol) {
        return """
                {"nombre":"Ana","correo":"%s","password":"%s","rol":"%s"}
                """.formatted(correo, password, rol);
    }
}
