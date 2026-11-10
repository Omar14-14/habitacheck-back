package com.habitacheck.backend.exception;

public class PublicacionInvalidaException extends RuntimeException {
    public PublicacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
