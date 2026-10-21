package com.example.quiropracticoapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        log.warn("Petición inválida: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Petición inválida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detalles = ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        log.warn("Error de validación: {}", detalles);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Error de validación de datos", detalles);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Intento de login fallido: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Usuario o contraseña incorrectos", ex.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Usuario o contraseña incorrectos", ex.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex) {
        log.warn("Cuenta bloqueada: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "La cuenta está bloqueada. Contacte con administración.", ex.getMessage());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabledException(DisabledException ex) {
        log.warn("Cuenta desactivada: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Esta cuenta ha sido eliminada o desactivada. Contacte con administración.", ex.getMessage());
    }

    @ExceptionHandler(HorarioOverlapException.class)
    public ResponseEntity<ApiErrorResponse> handleHorarioOverlap(HorarioOverlapException ex) {
        String codigoError = "CONFLICTO_" + ex.getTipoConflicto().toUpperCase();
        log.warn("Solapamiento de horario: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, codigoError, "Solapamiento detectado", ex.getMessage());
    }

    @ExceptionHandler(AgendaConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAgendaConflict(AgendaConflictException ex) {
        log.warn("Conflicto en agenda: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getCodigo(), "Conflicto de agenda", ex.getMessage());
    }

    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageQuotaExceededException(StorageQuotaExceededException ex) {
        log.error("Cuota de almacenamiento excedida: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "QUOTA_EXCEEDED", "Cuota de almacenamiento excedida", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(Exception ex) {
        log.error("Error crítico no controlado: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Ha ocurrido un error interno. Por favor intente más tarde.", ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String codigo, String mensaje, String detalles) {
        ApiErrorResponse response = new ApiErrorResponse(status.value(), codigo, mensaje, detalles);
        return new ResponseEntity<>(response, status);
    }
}
