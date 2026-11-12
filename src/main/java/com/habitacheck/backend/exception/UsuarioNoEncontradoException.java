package com.habitacheck.backend.exception;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long idUsuario) {
        super("No existe un usuario con id " + idUsuario);
    }
}

