package com.agrimandi.api.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidationException(BusinessValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BUSINESS_VALIDATION_ERROR")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .path(getPath(request))
                .details(details)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({org.springframework.security.core.AuthenticationException.class, org.springframework.security.authentication.BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message("Invalid credentials or unauthorized access")
                .path(getPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Authorization failures must surface as 403, not as a generic 500. Without this the
     * catch-all handler would mask role-based denials from clients.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .path(getPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .path(getPath(request))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private OffsetDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private List<String> details;
    }
}
