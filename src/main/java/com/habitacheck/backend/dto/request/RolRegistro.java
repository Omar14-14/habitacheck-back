package com.habitacheck.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RolRegistro {
    @JsonProperty("arrendatario")
    ARRENDATARIO("arrendatario"),

    @JsonProperty("arrendador")
    ARRENDADOR("arrendador");

    private final String nombre;

    RolRegistro(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
