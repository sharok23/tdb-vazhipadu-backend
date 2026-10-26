package com.tdb.vazhipadu.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OfferingControllerTest {

    private static final String OFFERINGS = "/api/temples/1/offerings";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReturns201WithLocation() throws Exception {
        String name = uniqueName();

        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, "30.00", true)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(OFFERINGS + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.templeId").value(1))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.price").value(30.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createReturns400ForInvalidFields() throws Exception {
        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"price\":0,\"active\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.active").exists());
    }

    @Test
    void createReturns400ForPriceWithMoreThanTwoDecimals() throws Exception {
        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), "10.505", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void createReturns404ForUnknownTemple() throws Exception {
        mockMvc.perform(post("/api/temples/999/offerings").contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), "30.00", true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Temple not found with id: 999"));
    }

    @Test
    void createReturns409ForDuplicateNameIgnoringCase() throws Exception {
        String name = uniqueName();
        createOffering(name, "30.00", true);

        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name.toUpperCase(), "40.00", true)))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsTempleOfferings() throws Exception {
        String name = uniqueName();
        createOffering(name, "30.00", false);

        mockMvc.perform(get(OFFERINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void listReturns404ForUnknownTemple() throws Exception {
        mockMvc.perform(get("/api/temples/999/offerings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateChangesPriceAndActiveFlag() throws Exception {
        String name = uniqueName();
        long id = createOffering(name, "30.00", true);

        mockMvc.perform(put(OFFERINGS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, "35.50", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(35.50))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateReturns404ForUnknownOffering() throws Exception {
        mockMvc.perform(put(OFFERINGS + "/999999").contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), "30.00", true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Offering not found with id: 999999"));
    }

    @Test
    void updateReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(put(OFFERINGS + "/abc").contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), "30.00", true)))
                .andExpect(status().isBadRequest());
    }

    private long createOffering(String name, String price, boolean active) throws Exception {
        String response = mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, price, active)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private static String body(String name, String price, boolean active) {
        return "{\"name\":\"%s\",\"price\":%s,\"active\":%s}".formatted(name, price, active);
    }

    private static String uniqueName() {
        return "Offering-" + UUID.randomUUID();
    }

    @Test
    void updateReturns409WhenRenamingToExistingName() throws Exception {
        String taken = uniqueName();
        createOffering(taken, "30.00", true);
        long id = createOffering(uniqueName(), "40.00", true);

        mockMvc.perform(put(OFFERINGS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(taken.toLowerCase(), "40.00", true)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturns400ForInvalidBody() throws Exception {
        long id = createOffering(uniqueName(), "30.00", true);

        mockMvc.perform(put(OFFERINGS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void createReturns400ForPriceAboveCap() throws Exception {
        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), "1000000.00", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }
}
