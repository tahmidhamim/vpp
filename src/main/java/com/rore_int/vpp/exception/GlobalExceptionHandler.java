package com.rore_int.vpp.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        // Strip the binding path prefix (e.g., "createBatteries.batteries[0].")
                        String fieldName = fieldError.getField().replaceAll(".*\\.", "");
                        return fieldName + ": " + error.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        logger.error("Validation error in request: {}", message);
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String propertyPath = violation.getPropertyPath().toString();
                    String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
                    return fieldName + ": " + violation.getMessage();
                })
                .collect(Collectors.joining("; "));
        logger.error("Constraint violation in request: {}", message);
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        logger.error("Validation error in service: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(DatabaseException ex) {
        logger.error("Database error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse("DATABASE_ERROR", "An error occurred while accessing the database");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Missing required parameter: " + ex.getParameterName();
        logger.error("Missing parameter in request: {}", message);
        ErrorResponse errorResponse = new ErrorResponse("MISSING_PARAMETER", message);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}