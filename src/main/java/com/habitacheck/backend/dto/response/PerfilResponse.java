package com.habitacheck.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Perfil completo del usuario autenticado.
 * <p>fotoPerfil es null si el usuario no ha subido imagen.</p>
 * <p>roles contiene al menos un elemento: "arrendatario", "arrendador" o "administrador".</p>
 */
public record PerfilResponse(
        Long idUsuario,
        String nombre,
        String correo,
        String telefono,
        String fotoPerfil,
        List<String> roles,
        String estado,
        OffsetDateTime fechaRegistro) {
}

