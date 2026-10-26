package com.tdb.vazhipadu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.constant.TimeZones;

@SpringBootTest
@AutoConfigureMockMvc
class OrderSearchControllerTest {

    private static final String ORDERS = "/api/temples/1/orders";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private long offeringId;

    @BeforeEach
    void setUp() throws Exception {
        String body = "{\"name\":\"Offering-%s\",\"price\":30.00,\"active\":true}".formatted(UUID.randomUUID());
        String response = mockMvc.perform(post("/api/temples/1/offerings")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        offeringId = objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void searchByPhoneReturnsOnlyThatDevoteesOrders() throws Exception {
        String phone = uniquePhone();
        long first = placeOrder(phone, tomorrow());
        long second = placeOrder(phone, tomorrow());
        placeOrder(uniquePhone(), tomorrow());

        mockMvc.perform(get(ORDERS).param("phone", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(first))
                .andExpect(jsonPath("$.content[1].id").value(second))
                .andExpect(jsonPath("$.content[0].phone").value(phone))
                .andExpect(jsonPath("$.content[1].phone").value(phone));
    }

    @Test
    void pageTwoContinuesPageOneWithoutRepeatsOrGaps() throws Exception {
        String phone = uniquePhone();
        List<Long> placed = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            placed.add(placeOrder(phone, tomorrow()));
        }

        List<Long> seen = new ArrayList<>();
        seen.addAll(pageIds(phone, 0));
        seen.addAll(pageIds(phone, 1));
        seen.addAll(pageIds(phone, 2));

        assertThat(seen).containsExactlyElementsOf(placed);
    }

    @Test
    void searchFiltersByDate() throws Exception {
        String phone = uniquePhone();
        placeOrder(phone, tomorrow());
        long later = placeOrder(phone, tomorrow().plusDays(1));

        mockMvc.perform(get(ORDERS).param("phone", phone).param("date", tomorrow().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(later));
    }

    @Test
    void searchFiltersByStatus() throws Exception {
        String phone = uniquePhone();
        placeOrder(phone, tomorrow());

        mockMvc.perform(get(ORDERS).param("phone", phone).param("status", "BOOKED"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(ORDERS).param("phone", phone).param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void searchWithoutFiltersUsesDefaultPage() throws Exception {
        placeOrder(uniquePhone(), tomorrow());

        mockMvc.perform(get(ORDERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void searchReturns400ForInvalidStatus() throws Exception {
        mockMvc.perform(get(ORDERS).param("status", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'PENDING' for parameter 'status'"));
    }

    @Test
    void searchReturns400ForInvalidDate() throws Exception {
        mockMvc.perform(get(ORDERS).param("date", "26-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value '26-10-2026' for parameter 'date'"));
    }

    @Test
    void searchReturns400ForPageSizeOverLimit() throws Exception {
        mockMvc.perform(get(ORDERS).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void searchReturns400ForNegativePage() throws Exception {
        mockMvc.perform(get(ORDERS).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").exists());
    }

    @Test
    void searchReturns400ForBadPhone() throws Exception {
        mockMvc.perform(get(ORDERS).param("phone", "12ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void searchReturns404ForUnknownTemple() throws Exception {
        mockMvc.perform(get("/api/temples/999/orders"))
                .andExpect(status().isNotFound());
    }

    private List<Long> pageIds(String phone, int page) throws Exception {
        String response = mockMvc.perform(get(ORDERS).param("phone", phone)
                        .param("page", String.valueOf(page)).param("size", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        for (JsonNode order : objectMapper.readTree(response).get("content")) {
            ids.add(order.get("id").asLong());
        }
        return ids;
    }

    private long placeOrder(String phone, LocalDate date) throws Exception {
        String body = """
                {"devoteeName":"Lakshmi","phone":"%s","nakshatra":"ROHINI","bookingDate":"%s",
                 "items":[{"offeringId":%d,"quantity":1}]}
                """.formatted(phone, date, offeringId);
        String response = mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private static String uniquePhone() {
        return "9" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
    }

    private static LocalDate tomorrow() {
        return LocalDate.now(TimeZones.IST).plusDays(1);
    }
}
