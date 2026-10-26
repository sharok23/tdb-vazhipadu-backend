package com.tdb.vazhipadu.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                Instant.now(),
                fieldErrors == null || fieldErrors.isEmpty() ? null : Collections.unmodifiableMap(fieldErrors));
    }
}
