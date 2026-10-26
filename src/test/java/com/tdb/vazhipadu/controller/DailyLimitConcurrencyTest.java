package com.tdb.vazhipadu.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.tdb.vazhipadu.constant.TimeZones;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DailyLimitConcurrencyTest {

    private static final int REQUESTS = 20;
    private static final int DAILY_LIMIT = 5;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void twentySimultaneousOrdersAgainstLimitOfFiveLetExactlyFiveSucceed() throws Exception {
        long offeringId = createOffering();
        String order = """
                {"devoteeName":"Lakshmi","phone":"9876543210","nakshatra":"ROHINI","bookingDate":"%s",
                 "items":[{"offeringId":%d,"quantity":1}]}
                """.formatted(LocalDate.now(TimeZones.IST).plusDays(5), offeringId);

        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatusCode>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(REQUESTS)) {
            for (int i = 0; i < REQUESTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return rest.postForEntity("/api/temples/1/orders", json(order), String.class).getStatusCode();
                }));
            }
            start.countDown();
        }

        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> result : results) {
            statuses.add(result.get());
        }
        assertThat(statuses).filteredOn(HttpStatus.CREATED::equals).hasSize(DAILY_LIMIT);
        assertThat(statuses).filteredOn(HttpStatus.CONFLICT::equals).hasSize(REQUESTS - DAILY_LIMIT);
    }

    private long createOffering() {
        String body = "{\"name\":\"Udayasthamana-%s\",\"price\":100.00,\"active\":true,\"dailyLimit\":%d}"
                .formatted(UUID.randomUUID(), DAILY_LIMIT);
        JsonNode created = rest.postForObject("/api/temples/1/offerings", json(body), JsonNode.class);
        return created.get("id").asLong();
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
