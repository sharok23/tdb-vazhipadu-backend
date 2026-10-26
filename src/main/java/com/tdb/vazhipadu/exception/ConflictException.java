package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
