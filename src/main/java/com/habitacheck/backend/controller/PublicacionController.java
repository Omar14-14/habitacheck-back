package com.habitacheck.backend.controller;

import com.habitacheck.backend.dto.request.CrearPublicacionRequest;
import com.habitacheck.backend.dto.response.PublicacionResponse;
import com.habitacheck.backend.service.PublicacionService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {
    private final PublicacionService publicaciones;

    public PublicacionController(PublicacionService publicaciones) {
        this.publicaciones = publicaciones;
    }

    @PostMapping
    public ResponseEntity<PublicacionResponse> crear(Principal principal,
            @Valid @RequestBody CrearPublicacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicaciones.crear(principal.getName(), request));
    }
}
