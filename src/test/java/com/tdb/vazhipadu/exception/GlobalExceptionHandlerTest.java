package com.tdb.vazhipadu.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tdb.vazhipadu.exception.ConflictException;
import com.tdb.vazhipadu.exception.NotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.TestItemsController.class)
class GlobalExceptionHandlerTest {

    record CreateItem(@NotBlank String name, @NotNull @Positive Integer quantity) {
    }

    @RestController
    @RequestMapping("/test/items")
    static class TestItemsController {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        CreateItem create(@Valid @RequestBody CreateItem body) {
            return body;
        }

        @GetMapping("/{id}")
        String get(@PathVariable Long id) {
            throw NotFoundException.of("Item", id);
        }

        @GetMapping("/positive/{id}")
        Long positive(@PathVariable @Positive Long id) {
            return id;
        }

        @GetMapping("/search")
        String search(@RequestParam String q) {
            return q;
        }

        @PostMapping("/duplicate")
        void duplicate() {
            throw new ConflictException("Item already exists");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("internal detail that must not leak");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validBodyIsAccepted() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pen\",\"quantity\":2,\"unknown\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pen"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/items"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
    }

    @Test
    void missingBodyReturns400() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongTypeInBodyReturns400() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pen\",\"quantity\":\"lots\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pathTypeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void constrainedPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/test/items/positive/-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.id").exists());
    }

    @Test
    void missingRequestParamReturns400() throws Exception {
        mockMvc.perform(get("/test/items/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'q'"));
    }

    @Test
    void notFoundExceptionReturns404() throws Exception {
        mockMvc.perform(get("/test/items/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item not found with id: 42"));
    }

    @Test
    void conflictReturns409() throws Exception {
        mockMvc.perform(post("/test/items/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(delete("/test/items"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.TEXT_PLAIN).content("pen"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/items/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
