package com.sistemagas.pedidos.exception.handler;

import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(BusinessException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("codigo", ex.getCodigo());
        errorData.put("status", ex.getStatus().value());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .exito(false)
                .mensaje(ex.getMessage())
                .data(errorData)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errores.put(campo, error.getDefaultMessage());
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .exito(false)
                .mensaje("Error de validacion")
                .data(errores)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "valor";
            errores.put(path, violation.getMessage());
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .exito(false)
                .mensaje("Error de validacion")
                .data(errores)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Error de formato en el request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El formato de los datos enviados es incorrecto o contiene valores inválidos."));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        String message = "No se puede completar la operacion por una restriccion de integridad de datos.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(message));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(
            org.springframework.dao.OptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("El recurso fue modificado por otra operacion. Intente nuevamente."));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessResourceFailure(
            org.springframework.dao.DataAccessResourceFailureException ex) {
        log.error("Fallo de conexion con la base de datos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "El servicio de base de datos no esta disponible en este momento. " +
                                "Por favor reintente en unos instantes."));
    }

    @ExceptionHandler(org.springframework.dao.QueryTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleQueryTimeout(
            org.springframework.dao.QueryTimeoutException ex) {
        log.error("Timeout de consulta a la base de datos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "La base de datos no respondio a tiempo. Por favor reintente."));
    }

    @ExceptionHandler(org.springframework.dao.CannotAcquireLockException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotAcquireLock(
            org.springframework.dao.CannotAcquireLockException ex) {
        log.warn("No se pudo adquirir lock en la base de datos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "El sistema esta procesando muchas operaciones concurrentes. " +
                                "Por favor reintente en unos segundos."));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene permisos para realizar esta operación"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Error no controlado", ex);

        ApiResponse<Void> response = ApiResponse.error("Error interno del servidor");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}