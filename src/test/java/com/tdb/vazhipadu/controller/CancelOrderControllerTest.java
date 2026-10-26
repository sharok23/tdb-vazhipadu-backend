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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.constant.TimeZones;

@SpringBootTest
@AutoConfigureMockMvc
class CancelOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        long orderId = orderId(placeOrder(createOffering(null), 2, today()));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancellingTwiceReturns409() throws Exception {
        long orderId = orderId(placeOrder(createOffering(null), 1, today()));
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order %d is already cancelled".formatted(orderId)));
    }

    @Test
    void cancelReturns404ForUnknownOrder() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/cancel", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: " + Long.MAX_VALUE));
    }

    @Test
    void cancelReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(post("/api/orders/abc/cancel")).andExpect(status().isBadRequest());
    }

    @Test
    void cancelledOrderFreesItsDailyLimit() throws Exception {
        long pooja = createOffering(5);
        LocalDate date = today().plusDays(2);
        long orderId = orderId(placeOrder(pooja, 5, date).andExpect(status().isCreated()));
        placeOrder(pooja, 1, date).andExpect(status().isConflict());

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)).andExpect(status().isOk());

        placeOrder(pooja, 5, date).andExpect(status().isCreated());
    }

    @Test
    void simultaneousCancelsGiveOne200AndOne409() throws Exception {
        long orderId = orderId(placeOrder(createOffering(null), 1, today()));
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> cancel = () -> {
            start.await();
            return mockMvc.perform(post("/api/orders/{id}/cancel", orderId)).andReturn().getResponse().getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = List.of(pool.submit(cancel), pool.submit(cancel));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
    }

    private static LocalDate today() {
        return LocalDate.now(TimeZones.IST);
    }

    private long orderId(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions placeOrder(long offeringId, int quantity, LocalDate date) throws Exception {
        String body = """
                {"devoteeName":"Lakshmi","phone":"9876543210","nakshatra":"ROHINI","bookingDate":"%s",
                 "items":[{"offeringId":%d,"quantity":%d}]}
                """.formatted(date, offeringId, quantity);
        return mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createOffering(Integer dailyLimit) throws Exception {
        String body = "{\"name\":\"Pooja-%s\",\"price\":100.00,\"active\":true,\"dailyLimit\":%s}"
                .formatted(UUID.randomUUID(), dailyLimit);
        String response = mockMvc.perform(post("/api/temples/1/offerings")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
