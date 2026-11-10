package com.habitacheck.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "propiedades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Propiedad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propiedad")
    private Long idPropiedad;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_arrendador", nullable = false)
    private Usuario arrendador;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
    @Column(name = "precio_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioMensual;
    @Column(nullable = false, length = 150)
    private String calle;
    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;
    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;
    @Column(nullable = false, length = 100)
    private String colonia;
    @Column(nullable = false, length = 100)
    private String municipio;
    @Column(name = "estado_ubicacion", nullable = false, length = 100)
    private String estadoUbicacion;
    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;
    @Column(nullable = false, length = 20)
    private String estado;
    @Column(name = "fecha_publicacion", nullable = false)
    private OffsetDateTime fechaPublicacion;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.PERSIST)
    @OrderBy("orden ASC")
    private List<PropiedadFotografia> fotografias = new ArrayList<>();

    // El catálogo se comparte: guardar una propiedad nunca crea ni borra servicios.
    @Getter(AccessLevel.NONE)
    @ManyToMany
    @JoinTable(name = "propiedad_servicio",
            joinColumns = @JoinColumn(name = "id_propiedad"),
            inverseJoinColumns = @JoinColumn(name = "id_servicio"))
    private Set<Servicio> servicios = new LinkedHashSet<>();

    public Propiedad(Usuario arrendador, String titulo, String descripcion,
                     BigDecimal precioMensual, String calle, String numeroExterior,
                     String numeroInterior, String colonia, String municipio,
                     String estadoUbicacion, String codigoPostal, OffsetDateTime fecha) {
        this.arrendador = arrendador;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precioMensual = precioMensual;
        this.calle = calle;
        this.numeroExterior = numeroExterior;
        this.numeroInterior = numeroInterior;
        this.colonia = colonia;
        this.municipio = municipio;
        this.estadoUbicacion = estadoUbicacion;
        this.codigoPostal = codigoPostal;
        this.estado = "disponible";
        this.fechaPublicacion = fecha;
    }

    public void agregarFotografia(String url) {
        if (fotografias.size() >= 30) {
            throw new IllegalArgumentException("Se permiten hasta 30 fotografías");
        }
        fotografias.add(new PropiedadFotografia(this, url,
                (short) (fotografias.size() + 1), fechaPublicacion));
    }

    public void incluirServicio(Servicio servicio) {
        servicios.add(servicio);
    }

    public List<PropiedadFotografia> getFotografias() {
        return Collections.unmodifiableList(fotografias);
    }

    public Set<Servicio> getServicios() {
        return Collections.unmodifiableSet(servicios);
    }
}
