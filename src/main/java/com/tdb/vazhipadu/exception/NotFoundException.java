package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public final class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException("%s not found with id: %s".formatted(resource, id));
    }
}
