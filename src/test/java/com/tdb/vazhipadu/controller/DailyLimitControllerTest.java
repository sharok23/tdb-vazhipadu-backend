package com.tdb.vazhipadu.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.constant.TimeZones;

@SpringBootTest
@AutoConfigureMockMvc
class DailyLimitControllerTest {

    private static final String OFFERINGS = "/api/temples/1/offerings";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void withFourOfFiveBookedAnOrderForTwoIsRejectedAndForOneSucceeds() throws Exception {
        JsonNode pooja = createOffering(5);
        long id = pooja.get("id").asLong();
        LocalDate date = LocalDate.now(TimeZones.IST).plusDays(3);
        placeOrder(id, 4, date).andExpect(status().isCreated());

        placeOrder(id, 2, date)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Daily limit reached for '%s' on %s: 1 of 5 left"
                        .formatted(pooja.get("name").asText(), date)));
        placeOrder(id, 1, date).andExpect(status().isCreated());
    }

    @Test
    void limitIsCountedPerDate() throws Exception {
        long pooja = createOffering(1).get("id").asLong();
        LocalDate date = LocalDate.now(TimeZones.IST).plusDays(3);
        placeOrder(pooja, 1, date).andExpect(status().isCreated());

        placeOrder(pooja, 1, date.plusDays(1)).andExpect(status().isCreated());
    }

    @Test
    void offeringWithoutLimitAcceptsAnyQuantity() throws Exception {
        long archana = createOffering(null).get("id").asLong();

        placeOrder(archana, 100, LocalDate.now(TimeZones.IST)).andExpect(status().isCreated());
    }

    @Test
    void createReturnsDailyLimit() throws Exception {
        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(offering("P-" + UUID.randomUUID(), 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dailyLimit").value(50));
    }

    @Test
    void createReturns400ForZeroDailyLimit() throws Exception {
        mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(offering("P-" + UUID.randomUUID(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dailyLimit").exists());
    }

    @Test
    void updateCanRemoveDailyLimit() throws Exception {
        JsonNode pooja = createOffering(5);

        mockMvc.perform(put(OFFERINGS + "/" + pooja.get("id").asLong()).contentType(MediaType.APPLICATION_JSON)
                        .content(offering(pooja.get("name").asText(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").doesNotExist());
    }

    private ResultActions placeOrder(long offeringId, int quantity, LocalDate date) throws Exception {
        String body = """
                {"devoteeName":"Lakshmi","phone":"9876543210","nakshatra":"ROHINI","bookingDate":"%s",
                 "items":[{"offeringId":%d,"quantity":%d}]}
                """.formatted(date, offeringId, quantity);
        return mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode createOffering(Integer dailyLimit) throws Exception {
        String response = mockMvc.perform(post(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                        .content(offering("Pooja-" + UUID.randomUUID(), dailyLimit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private static String offering(String name, Integer dailyLimit) {
        return "{\"name\":\"%s\",\"price\":100.00,\"active\":true,\"dailyLimit\":%s}".formatted(name, dailyLimit);
    }
}
