package com.habitacheck.backend.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail correoDuplicado(CorreoYaRegistradoException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException exception) {
        Map<String, String> errores = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String campo = error.getField().equals("passwordUtf8Valida")
                    ? "password" : error.getField();
            errores.putIfAbsent(campo, error.getDefaultMessage());
        });
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Revisa los datos del registro");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El JSON es inválido. El rol debe ser arrendatario o arrendador");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail validacionServicio() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Revisa los datos del registro");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException exception) {
        // Solo el índice del correo equivale a un conflicto de registro.
        // Otras violaciones de integridad no deben reportarse como correo duplicado.
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof org.hibernate.exception.ConstraintViolationException constraint
                    && "ux_usuarios_correo_lower".equals(constraint.getConstraintName())) {
                return ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT, "El correo ya está registrado");
            }
        }
        return errorInterno(exception);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInterno(Exception exception) {
        LOG.error("No se pudo completar la operación", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo completar la operación. Inténtalo más tarde");
    }
}
