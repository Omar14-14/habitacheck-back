package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.CrearPublicacionRequest;
import com.habitacheck.backend.dto.response.PublicacionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface PublicacionService {
    // El correo debe provenir de la identidad autenticada, nunca del cuerpo HTTP.
    PublicacionResponse crear(@NotBlank String correoAutenticado,
                              @NotNull @Valid CrearPublicacionRequest request);
}
