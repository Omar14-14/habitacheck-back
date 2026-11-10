package com.habitacheck.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PublicacionResponse(
        Long idPropiedad, Long idArrendador, String titulo, String descripcion,
        BigDecimal precioMensual, String calle, String numeroExterior, String numeroInterior,
        String colonia, String municipio, String estadoUbicacion, String codigoPostal,
        String estado, OffsetDateTime fechaPublicacion,
        List<FotografiaResponse> fotografias, List<ServicioResponse> servicios) {

    public record FotografiaResponse(Long idFotografia, String url, short orden,
                                     OffsetDateTime fechaCarga) { }
    public record ServicioResponse(Long idServicio, String nombre) { }
}
