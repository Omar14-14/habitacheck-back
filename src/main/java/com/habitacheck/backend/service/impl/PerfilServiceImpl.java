package com.habitacheck.backend.service.impl;

import com.habitacheck.backend.dto.request.ActualizarPerfilRequest;
import com.habitacheck.backend.dto.response.PerfilResponse;
import com.habitacheck.backend.entity.Usuario;
import com.habitacheck.backend.exception.UsuarioNoEncontradoException;
import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import com.habitacheck.backend.service.PerfilService;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Service
@Validated
public class PerfilServiceImpl implements PerfilService {

    private static final long MAX_BYTES = 5 * 1024 * 1024L; // 5 MB

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    public PerfilServiceImpl(UsuarioRepository usuarioRepository,
                             UsuarioRolRepository usuarioRolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioRolRepository = usuarioRolRepository;
    }

    // ── Consulta ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPerfil(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        return toResponse(usuario);
    }

    // ── Editar nombre / teléfono ──────────────────────────────────────────────

    @Override
    @Transactional
    public PerfilResponse actualizarPerfil(Long idUsuario, ActualizarPerfilRequest request) {
        Usuario usuario = buscarOFallar(idUsuario);
        usuario.actualizarNombre(request.nombre());
        usuario.actualizarTelefono(request.telefono());
        usuarioRepository.saveAndFlush(usuario);
        return toResponse(usuario);
    }

    // ── Foto de perfil ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PerfilResponse actualizarFoto(Long idUsuario, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Debes enviar una imagen");
        }
        if (foto.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 5 MB");
        }

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        byte[] bytes;
        try {
            bytes = foto.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen", e);
        }

        // Guardamos como Data URI para que el cliente pueda usarla directamente
        // (compatible con foto desde galería o desde cámara).
        String dataUri = "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);

        Usuario usuario = buscarOFallar(idUsuario);
        usuario.actualizarFotoPerfil(dataUri);
        usuarioRepository.saveAndFlush(usuario);
        return toResponse(usuario);
    }

    @Override
    @Transactional
    public PerfilResponse eliminarFoto(Long idUsuario) {
        Usuario usuario = buscarOFallar(idUsuario);
        usuario.actualizarFotoPerfil(null);
        usuarioRepository.saveAndFlush(usuario);
        return toResponse(usuario);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Usuario buscarOFallar(Long idUsuario) {
        return usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));
    }

    private PerfilResponse toResponse(Usuario usuario) {
        List<String> roles = usuarioRolRepository.findByUsuario(usuario)
                .stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();

        return new PerfilResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getFotoPerfil(),
                roles,
                usuario.getEstado(),
                usuario.getFechaRegistro());
    }
}

