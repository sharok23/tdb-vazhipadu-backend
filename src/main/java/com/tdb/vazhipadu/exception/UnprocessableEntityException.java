package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public final class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
