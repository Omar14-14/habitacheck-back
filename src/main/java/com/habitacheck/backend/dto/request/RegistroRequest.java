package com.habitacheck.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Size(max = 25, message = "El teléfono admite hasta 25 caracteres")
        String telefono,

        @NotNull(message = "Debes elegir arrendatario o arrendador")
        RolRegistro rol) {

    public RegistroRequest {
        nombre = nombre == null ? null : nombre.strip();
        correo = correo == null ? null : correo.strip().toLowerCase(Locale.ROOT);
        telefono = telefono == null || telefono.isBlank() ? null : telefono.strip();
    }

    // BCrypt limita la entrada a 72 bytes; un carácter puede ocupar varios bytes.
    @JsonIgnore
    @AssertTrue(message = "La contraseña admite hasta 72 bytes en UTF-8")
    public boolean isPasswordUtf8Valida() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "RegistroRequest[password=PROTEGIDA]";
    }
}
