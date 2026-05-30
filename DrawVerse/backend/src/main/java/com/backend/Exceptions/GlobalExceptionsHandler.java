package com.backend.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionsHandler {

    @ExceptionHandler(RoomExceptions.RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomExceptions.RoomNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RoomExceptions.RoomFullException.class)
    public ResponseEntity<ErrorResponse> handleRoomFull(RoomExceptions.RoomFullException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RoomExceptions.RoomNotJoinableException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotJoinable(RoomExceptions.RoomNotJoinableException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RoomExceptions.PlayerAlreadyInRoomException.class)
    public ResponseEntity<ErrorResponse> handlePlayerAlreadyInRoom(RoomExceptions.PlayerAlreadyInRoomException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RoomExceptions.NotHostException.class)
    public ResponseEntity<ErrorResponse> handleNotHost(RoomExceptions.NotHostException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Handles @Valid annotation failures on request bodies
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private Map<String, String> fieldErrors;
        private LocalDateTime timestamp;
    }
}
