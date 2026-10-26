package com.chiangraipass.auth.controller;

import java.util.Map;


import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.chiangraipass.privacy.controller.PrivacyPreferenceController;

/** Returns the reason for a rejected auth request as {"message": "..."} so the UI can show it. */
@RestControllerAdvice(assignableTypes = {AuthController.class, PrivacyPreferenceController.class})
public class AuthExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException error) {
        String message = error.getReason() != null
                ? error.getReason()
                : HttpStatus.valueOf(error.getStatusCode().value()).getReasonPhrase();
        return respond(error.getStatusCode().value(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("Some of the information entered is not valid.");
        return respond(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException error) {
        return respond(HttpStatus.BAD_REQUEST.value(), "The request could not be read. Please try again.");
    }

    private ResponseEntity<Map<String, String>> respond(int status, String message) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("message", message));
    }
}