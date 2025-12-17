package com.example.quiropracticoapi.exception;

import com.example.quiropracticoapi.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // Manejar ResourceNotFoundException (Devolver 404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorDto error = new ErrorDto(
                ex.getMessage(),
                "NOT_FOUND",
                HttpStatus.NOT_FOUND.value()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Manejar IllegalArgumentException (Devolver 400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorDto error = new ErrorDto(
                ex.getMessage(),
                "BAD_REQUEST",
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Maneja las credenciales incorrectas
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException ex) {
        String mensaje = ex.getMessage();
        if (mensaje == null || mensaje.equals("Bad credentials")) {
            mensaje = "Usuario o contraseña incorrectos";
        }
        ErrorDto error = new ErrorDto(
                mensaje,
                "UNAUTHORIZED",
                HttpStatus.UNAUTHORIZED.value()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Manejar usuario no encontrado
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorDto> handleUsernameNotFound(UsernameNotFoundException ex) {
        ErrorDto error = new ErrorDto(
                "Usuario o contraseña incorrectos",
                "UNAUTHORIZED",
                HttpStatus.UNAUTHORIZED.value()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Manejar Cuenta Bloqueada
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorDto> handleLocked(LockedException ex) {
        ErrorDto error = new ErrorDto(
                "La cuenta está bloqueada. Contacte con administración.",
                "ACCOUNT_LOCKED",
                HttpStatus.FORBIDDEN.value()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Manejar cuenta desactivada
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorDto> handleDisabledException(DisabledException ex) {
        ErrorDto error = new ErrorDto(
                "Esta cuenta ha sido eliminada o desactivada. Contacte con administración.",
                "ACCOUNT_DISABLED",
                HttpStatus.FORBIDDEN.value()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Manejar cualquier otro error no controlado (Devolver 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGlobalException(Exception ex) {
        logger.error("Error no controlado: ", ex);

        ErrorDto error = new ErrorDto(
                "Ha ocurrido un error interno. Por favor intente más tarde.",
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejar errores de validación (@Valid: @Future, @NotNull, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();

        ErrorDto error = new ErrorDto(
                mensaje,
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
