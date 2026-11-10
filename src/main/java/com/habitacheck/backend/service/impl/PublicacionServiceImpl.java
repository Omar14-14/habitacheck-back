package com.habitacheck.backend.service.impl;

import com.habitacheck.backend.dto.request.CrearPublicacionRequest;
import com.habitacheck.backend.dto.response.PublicacionResponse;
import com.habitacheck.backend.dto.response.PublicacionResponse.FotografiaResponse;
import com.habitacheck.backend.dto.response.PublicacionResponse.ServicioResponse;
import com.habitacheck.backend.entity.Propiedad;
import com.habitacheck.backend.exception.PublicacionInvalidaException;
import com.habitacheck.backend.repository.*;
import com.habitacheck.backend.service.PublicacionService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PublicacionServiceImpl implements PublicacionService {
    private final UsuarioRepository usuarios;
    private final UsuarioRolRepository roles;
    private final ServicioRepository servicios;
    private final PropiedadRepository propiedades;

    public PublicacionServiceImpl(UsuarioRepository usuarios, UsuarioRolRepository roles,
                                 ServicioRepository servicios, PropiedadRepository propiedades) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.servicios = servicios;
        this.propiedades = propiedades;
    }

    @Override
    @Transactional
    public PublicacionResponse crear(String correoAutenticado, CrearPublicacionRequest request) {
        var arrendador = usuarios.findByCorreoIgnoreCase(correoAutenticado)
                .filter(usuario -> "activo".equals(usuario.getEstado()))
                .orElseThrow(() -> new AccessDeniedException("La cuenta no está activa"));
        if (!roles.existsByUsuario_IdUsuarioAndRol_Nombre(arrendador.getIdUsuario(), "arrendador")) {
            throw new AccessDeniedException("Se requiere el rol arrendador para publicar");
        }

        var ids = new HashSet<>(request.servicios());
        if (ids.size() != request.servicios().size()) {
            throw new PublicacionInvalidaException("No se permite repetir servicios");
        }
        var incluidos = servicios.findAllById(ids);
        if (incluidos.size() != ids.size()
                || incluidos.stream().anyMatch(servicio -> !"activo".equals(servicio.getEstado()))) {
            throw new PublicacionInvalidaException("Los servicios deben existir y estar activos");
        }

        var propiedad = new Propiedad(arrendador, request.titulo(), request.descripcion(),
                request.precioMensual(), request.calle(), request.numeroExterior(),
                request.numeroInterior(), request.colonia(), request.municipio(),
                request.estadoUbicacion(), request.codigoPostal(), OffsetDateTime.now(ZoneOffset.UTC));
        request.fotografias().forEach(propiedad::agregarFotografia);
        incluidos.forEach(propiedad::incluirServicio);
        propiedades.saveAndFlush(propiedad);

        return new PublicacionResponse(propiedad.getIdPropiedad(), arrendador.getIdUsuario(),
                propiedad.getTitulo(), propiedad.getDescripcion(), propiedad.getPrecioMensual(),
                propiedad.getCalle(), propiedad.getNumeroExterior(), propiedad.getNumeroInterior(),
                propiedad.getColonia(), propiedad.getMunicipio(), propiedad.getEstadoUbicacion(),
                propiedad.getCodigoPostal(), propiedad.getEstado(), propiedad.getFechaPublicacion(),
                propiedad.getFotografias().stream().map(foto -> new FotografiaResponse(
                        foto.getIdFotografia(), foto.getUrl(), foto.getOrden(), foto.getFechaCarga())).toList(),
                propiedad.getServicios().stream().map(servicio -> new ServicioResponse(
                        servicio.getIdServicio(), servicio.getNombre()))
                        .sorted(Comparator.comparing(ServicioResponse::idServicio)).toList());
    }
}
