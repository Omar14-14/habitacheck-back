package com.habitacheck.backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CrearPublicacionRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 20000) String descripcion,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioMensual,
        @NotBlank @Size(max = 150) String calle,
        @NotBlank @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @NotBlank @Size(max = 100) String colonia,
        @NotBlank @Size(max = 100) String municipio,
        @NotBlank @Size(max = 100) String estadoUbicacion,
        @NotBlank @Pattern(regexp = "[0-9]{5}") String codigoPostal,
        @Size(max = 30) List<@NotBlank @Size(max = 2048)
                @Pattern(regexp = "https://[^\\s/?#]+(?:[/?#][^\\s]*)?",
                        message = "La fotografía debe tener una URL HTTPS") String> fotografias,
        @Size(max = 100) List<@NotNull @Positive Long> servicios) {

    public CrearPublicacionRequest {
        titulo = limpiar(titulo);
        descripcion = limpiar(descripcion);
        calle = limpiar(calle);
        numeroExterior = limpiar(numeroExterior);
        numeroInterior = numeroInterior == null || numeroInterior.isBlank()
                ? null : numeroInterior.strip();
        colonia = limpiar(colonia);
        municipio = limpiar(municipio);
        estadoUbicacion = limpiar(estadoUbicacion);
        codigoPostal = limpiar(codigoPostal);
        // List.copyOf rechaza elementos null antes de que Bean Validation pueda reportarlos.
        fotografias = fotografias == null ? List.of()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(fotografias));
        servicios = servicios == null ? List.of()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(servicios));
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.strip();
    }
}
