package com.tdb.vazhipadu.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorResponseTest {

    @Test
    void ofFillsStatusReasonPathAndTimestamp() {
        ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, "missing", "/api/x", null);

        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("missing");
        assertThat(body.path()).isEqualTo("/api/x");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void nullOrEmptyFieldErrorsAreOmitted() {
        assertThat(ErrorResponse.of(HttpStatus.BAD_REQUEST, "m", "/p", null).fieldErrors()).isNull();
        assertThat(ErrorResponse.of(HttpStatus.BAD_REQUEST, "m", "/p", Map.of()).fieldErrors()).isNull();
    }

    @Test
    void fieldErrorsAreCopiedReadOnly() {
        Map<String, String> errors = new HashMap<>(Map.of("name", "must not be blank"));
        Map<String, String> result = ErrorResponse.of(HttpStatus.BAD_REQUEST, "m", "/p", errors).fieldErrors();

        assertThat(result).containsEntry("name", "must not be blank");
        assertThatThrownBy(() -> result.put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }
}
