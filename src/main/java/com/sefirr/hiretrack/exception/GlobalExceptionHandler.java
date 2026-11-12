package com.sefirr.hiretrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid email or password", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Authentication failed", req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "Request conflicts with existing data", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(err.getField(), err.getDefaultMessage());
        }
        for (ObjectError err : ex.getBindingResult().getGlobalErrors()) {
            fieldErrors.putIfAbsent(err.getObjectName(), err.getDefaultMessage());
        }
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, "Validation failed", req);
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(err ->
                fieldErrors.putIfAbsent(result.getMethodParameter().getParameterName(), err.getDefaultMessage())));
        ErrorResponse body = build(HttpStatus.BAD_REQUEST, "Validation failed", req);
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed request body or invalid field value", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds the 10 MB limit", req);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorage(StorageException ex, HttpServletRequest req) {
        log.error("Storage error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "File storage error", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Spring MVC exceptions (404 no route, 405 wrong method, 415, missing params...) carry their own status
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            String detail = springError.getBody().getDetail();
            return respond(HttpStatus.valueOf(status.value()), detail, req);
        }
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        // Never leak internal exception messages to clients
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(build(status, message, req));
    }

    private ErrorResponse build(HttpStatus status, String message, HttpServletRequest req) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> fieldErrors;
    }
}
