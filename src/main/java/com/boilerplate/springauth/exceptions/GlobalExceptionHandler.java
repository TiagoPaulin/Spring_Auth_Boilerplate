package com.boilerplate.springauth.exceptions;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFound (ResourceNotFoundException e, HttpServletRequest request) {

        String error = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;

        StandardError standardError = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(standardError);

    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<StandardError> badRequest(BadRequestException e, HttpServletRequest request) {

        String error = "Data Error";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        StandardError standardError = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(standardError);

    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> forbidden(ForbiddenException e, HttpServletRequest request) {

        String error = "Forbidden Request";
        HttpStatus status = HttpStatus.FORBIDDEN;

        StandardError standardError = new StandardError(Instant.now(), status.value(), error, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(status).body(standardError);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validationErrors(MethodArgumentNotValidException e, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        StringBuilder message = new StringBuilder();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            message.append(error.getField())
                .append(": ")
                .append(error.getDefaultMessage())
                .append("; ");
        }

        StandardError err = new StandardError(
            Instant.now(),
            status.value(),
            "Validation error",
            message.toString(),
            request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
        
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        StandardError err = new StandardError(
            Instant.now(), 
            status.value(), 
            "Access Denied", 
            "You do not have permission to access this resource", 
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<StandardError> handleAuthentication(org.springframework.security.core.AuthenticationException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED; 
        
        StandardError err = new StandardError(
            Instant.now(),
            status.value(),
            "Authentication Error",
            "Incorrect email or password", 
            request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> globalError(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(
            Instant.now(),
            status.value(),
            "Internal Server Error",
            "An unexpected error occurred on the server", 
            request.getRequestURI()
        );

        e.printStackTrace(); 
        return ResponseEntity.status(status).body(err);
    }

}
