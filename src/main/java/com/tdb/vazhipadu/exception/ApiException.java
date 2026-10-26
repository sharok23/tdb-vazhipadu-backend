package com.tdb.vazhipadu.exception;

import org.springframework.http.HttpStatus;

public abstract sealed class ApiException extends RuntimeException
        permits NotFoundException, ConflictException, BadRequestException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
