package com.habitacheck.backend.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Campos editables del perfil. Ambos son opcionales:
 * si se omite un campo (null) no se modifica.
 */
public record ActualizarPerfilRequest(

        @Size(min = 1, max = 150, message = "El nombre debe tener entre 1 y 150 caracteres")
        String nombre,

        @Size(max = 25, message = "El teléfono admite hasta 25 caracteres")
        String telefono) {

    public ActualizarPerfilRequest {
        nombre   = nombre   == null ? null : nombre.strip();
        telefono = telefono == null ? null : telefono.strip();
    }
}

