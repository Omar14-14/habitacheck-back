package com.habitacheck.backend.controller;

import com.habitacheck.backend.dto.response.PerfilResponse;
import com.habitacheck.backend.exception.UsuarioNoEncontradoException;
import com.habitacheck.backend.service.PerfilService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerfilController.class)
class PerfilControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PerfilService perfilService;

    private static final PerfilResponse PERFIL_MOCK = new PerfilResponse(
            1L, "Ana López", "ana@example.com", "5512345678",
            null, List.of("arrendatario"), "activo",
            OffsetDateTime.now(ZoneOffset.UTC));

    // ── GET /api/perfil ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void obtenerPerfilDevuelve200ConTodosLosCampos() throws Exception {
        when(perfilService.obtenerPerfil(1L)).thenReturn(PERFIL_MOCK);

        mvc.perform(get("/api/perfil").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana López"))
                .andExpect(jsonPath("$.correo").value("ana@example.com"))
                .andExpect(jsonPath("$.telefono").value("5512345678"))
                .andExpect(jsonPath("$.roles[0]").value("arrendatario"))
                .andExpect(jsonPath("$.estado").value("activo"));
    }

    @Test
    @WithMockUser
    void obtenerPerfilDevuelve404SiNoExiste() throws Exception {
        when(perfilService.obtenerPerfil(99L))
                .thenThrow(new UsuarioNoEncontradoException(99L));

        mvc.perform(get("/api/perfil").param("idUsuario", "99"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/perfil ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void actualizarPerfilDevuelve200() throws Exception {
        var actualizado = new PerfilResponse(
                1L, "María García", "ana@example.com", "5599887766",
                null, List.of("arrendatario"), "activo",
                OffsetDateTime.now(ZoneOffset.UTC));

        when(perfilService.actualizarPerfil(eq(1L), any())).thenReturn(actualizado);

        mvc.perform(patch("/api/perfil")
                        .param("idUsuario", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"María García\",\"telefono\":\"5599887766\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("María García"))
                .andExpect(jsonPath("$.telefono").value("5599887766"));
    }

    // ── PUT /api/perfil/foto ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void subirFotoDevuelve200ConDataUri() throws Exception {
        var conFoto = new PerfilResponse(
                1L, "Ana López", "ana@example.com", "5512345678",
                "data:image/jpeg;base64,/9j/4AAQ==",
                List.of("arrendatario"), "activo",
                OffsetDateTime.now(ZoneOffset.UTC));

        when(perfilService.actualizarFoto(eq(1L), any())).thenReturn(conFoto);

        MockMultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/perfil/foto")
                        .file(foto)
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoPerfil").value("data:image/jpeg;base64,/9j/4AAQ=="));
    }

    // ── DELETE /api/perfil/foto ───────────────────────────────────────────────

    @Test
    @WithMockUser
    void eliminarFotoDevuelve200ConFotoNull() throws Exception {
        when(perfilService.eliminarFoto(1L)).thenReturn(PERFIL_MOCK);

        mvc.perform(delete("/api/perfil/foto").param("idUsuario", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana López"));
    }
}
