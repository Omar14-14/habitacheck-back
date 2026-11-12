package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.ActualizarPerfilRequest;
import com.habitacheck.backend.dto.response.PerfilResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public interface PerfilService {

    /** Devuelve el perfil completo del usuario: nombre, correo, teléfono, foto y rol(es). */
    PerfilResponse obtenerPerfil(@NotNull Long idUsuario);

    /** Actualiza nombre y/o teléfono. Los campos null no se tocan. */
    PerfilResponse actualizarPerfil(@NotNull Long idUsuario,
                                    @NotNull @Valid ActualizarPerfilRequest request);

    /**
     * Guarda la imagen como Base64 en foto_perfil.
     * Acepta tanto archivos del selector como capturas de cámara: el cliente
     * siempre envía los bytes; el backend no distingue el origen.
     */
    PerfilResponse actualizarFoto(@NotNull Long idUsuario, @NotNull MultipartFile foto);

    /** Elimina la foto de perfil (pone foto_perfil = null). */
    PerfilResponse eliminarFoto(@NotNull Long idUsuario);
}

