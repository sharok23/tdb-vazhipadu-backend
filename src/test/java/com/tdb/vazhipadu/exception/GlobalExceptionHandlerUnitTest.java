package com.tdb.vazhipadu.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerUnitTest {

    record Item(@NotBlank String name) {
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/items");
    }

    @Test
    void apiExceptionUsesItsStatusAndMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleApi(new BadRequestException("bad input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("bad input");
        assertThat(response.getBody().path()).isEqualTo("/api/items");
    }

    @Test
    void bodyValidationCollectsFieldAndGlobalErrorsKeepingFirstPerField() throws NoSuchMethodException {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Item(""), "item");
        result.rejectValue("name", "NotBlank", "must not be blank");
        result.rejectValue("name", "Size", "too short");
        result.reject("Range", "end must be after start");
        MethodParameter parameter = new MethodParameter(Item.class.getDeclaredMethod("name"), -1);

        ResponseEntity<ErrorResponse> response =
                handler.handleBodyValidation(new MethodArgumentNotValidException(parameter, result), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors())
                .containsEntry("name", "must not be blank")
                .containsEntry("item", "end must be after start")
                .hasSize(2);
    }

    @Test
    void constraintViolationUsesLastPathSegmentAsField() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Item>> violations = validator.validate(new Item(" "));

        ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().fieldErrors()).containsKey("name");
    }

    @Test
    void illegalArgumentReturns400WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("page must be >= 0"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("page must be >= 0");
    }

    @Test
    void unknownResourceReturns404NamingMethodAndPath() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResource(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("No endpoint POST /api/items");
    }

    @Test
    void unexpectedErrorHidesDetails() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("db password is hunter2"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    void dataIntegrityViolationReturns409() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Request conflicts with existing data");
    }

    @Test
    void lockTimeoutReturns503() {
        ResponseEntity<ErrorResponse> response =
                handler.handleLockTimeout(new PessimisticLockingFailureException("timeout"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).isEqualTo("Resource is busy with other requests, please retry");
    }
}
