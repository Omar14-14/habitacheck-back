package com.habitacheck.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "propiedad_fotografia", uniqueConstraints = @UniqueConstraint(
        name = "uq_propiedad_fotografia_orden", columnNames = {"id_propiedad", "orden"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropiedadFotografia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fotografia")
    private Long idFotografia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false)
    private Propiedad propiedad;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
    @Column(nullable = false)
    private short orden;
    @Column(name = "fecha_carga", nullable = false)
    private OffsetDateTime fechaCarga;

    PropiedadFotografia(Propiedad propiedad, String url, short orden, OffsetDateTime fechaCarga) {
        this.propiedad = propiedad;
        this.url = url;
        this.orden = orden;
        this.fechaCarga = fechaCarga;
    }
}
