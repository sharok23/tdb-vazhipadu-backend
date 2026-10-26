package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public final class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
