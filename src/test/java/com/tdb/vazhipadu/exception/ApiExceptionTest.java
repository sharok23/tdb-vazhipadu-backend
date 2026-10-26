package com.tdb.vazhipadu.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void notFoundOfBuildsMessageAndStatus() {
        NotFoundException ex = NotFoundException.of("Task", 7L);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Task not found with id: 7");
    }

    @Test
    void notFoundKeepsCustomMessage() {
        assertThat(new NotFoundException("gone").getMessage()).isEqualTo("gone");
    }

    @Test
    void conflictHas409() {
        ConflictException ex = new ConflictException("exists");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("exists");
    }

    @Test
    void badRequestHas400() {
        BadRequestException ex = new BadRequestException("bad");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("bad");
    }
}
