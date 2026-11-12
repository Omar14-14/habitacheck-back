package com.habitacheck.backend.controller;

import com.habitacheck.backend.dto.request.ActualizarPerfilRequest;
import com.habitacheck.backend.dto.response.PerfilResponse;
import com.habitacheck.backend.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Perfil del usuario: consulta, edición de datos y gestión de foto.
 *
 * <p><b>Autenticación temporal:</b> el {@code idUsuario} se recibe como query-param
 * mientras el equipo implementa JWT. Al integrar el token, se reemplaza por
 * {@code SecurityContextHolder.getContext().getAuthentication().getName()}.</p>
 *
 * <p><b>Foto de perfil:</b> el cliente envía los bytes independientemente de si
 * el usuario eligió la galería o tomó la foto con la cámara; el backend no
 * distingue el origen.</p>
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /**
     * GET /api/perfil?idUsuario={id}
     * Devuelve nombre, correo, teléfono, foto (Base64 o null) y rol(es).
     */
    @GetMapping
    public ResponseEntity<PerfilResponse> obtenerPerfil(
            @RequestParam Long idUsuario) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(idUsuario));
    }

    /**
     * PATCH /api/perfil?idUsuario={id}
     * Actualiza nombre y/o teléfono. Los campos null no se modifican.
     */
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @RequestParam Long idUsuario,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(idUsuario, request));
    }

    /**
     * PUT /api/perfil/foto?idUsuario={id}
     * Sube o reemplaza la foto de perfil.
     * Content-Type: multipart/form-data, campo "foto" con la imagen.
     * El cliente envía el mismo campo tanto si abrió la galería como si capturó
     * la imagen con la cámara del dispositivo.
     */
    @PutMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PerfilResponse> actualizarFoto(
            @RequestParam Long idUsuario,
            @RequestPart("foto") MultipartFile foto) {
        return ResponseEntity.ok(perfilService.actualizarFoto(idUsuario, foto));
    }

    /**
     * DELETE /api/perfil/foto?idUsuario={id}
     * Elimina la foto de perfil (pone foto_perfil = null).
     */
    @DeleteMapping("/foto")
    public ResponseEntity<PerfilResponse> eliminarFoto(
            @RequestParam Long idUsuario) {
        return ResponseEntity.ok(perfilService.eliminarFoto(idUsuario));
    }
}

