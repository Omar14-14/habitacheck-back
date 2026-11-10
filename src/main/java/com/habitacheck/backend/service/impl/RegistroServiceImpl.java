package com.habitacheck.backend.service.impl;

import com.habitacheck.backend.dto.request.RegistroRequest;
import com.habitacheck.backend.dto.response.RegistroResponse;
import com.habitacheck.backend.entity.Rol;
import com.habitacheck.backend.entity.Usuario;
import com.habitacheck.backend.entity.UsuarioRol;
import com.habitacheck.backend.exception.CorreoYaRegistradoException;
import com.habitacheck.backend.repository.RolRepository;
import com.habitacheck.backend.repository.UsuarioRepository;
import com.habitacheck.backend.repository.UsuarioRolRepository;
import com.habitacheck.backend.service.RegistroService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class RegistroServiceImpl implements RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            UsuarioRolRepository usuarioRolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioRolRepository = usuarioRolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByCorreoIgnoreCase(request.correo())) {
            throw new CorreoYaRegistradoException();
        }

        Rol rol = rolRepository.findByNombre(request.rol().getNombre())
                .orElseThrow(() -> new IllegalStateException(
                        "El rol de registro no está cargado en la base de datos"));

        OffsetDateTime fecha = OffsetDateTime.now(ZoneOffset.UTC);
        String hash = passwordEncoder.encode(request.password());

        Usuario usuario = new Usuario(
                request.nombre(), request.correo(), hash, request.telefono(), fecha);
        usuarioRepository.saveAndFlush(usuario);

        usuarioRolRepository.saveAndFlush(new UsuarioRol(usuario, rol, fecha));

        return new RegistroResponse(
                usuario.getIdUsuario(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getTelefono(), usuario.getEstado(),
                List.of(rol.getNombre()), usuario.getFechaRegistro());
    }
}
