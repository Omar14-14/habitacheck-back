package com.habitacheck.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record RegistroResponse(
        Long idUsuario,
        String nombre,
        String correo,
        String telefono,
        String estado,
        List<String> roles,
        OffsetDateTime fechaRegistro) {
}
