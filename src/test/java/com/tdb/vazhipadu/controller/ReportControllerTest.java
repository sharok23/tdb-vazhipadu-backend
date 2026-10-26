package com.tdb.vazhipadu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.constant.Nakshatra;
import com.tdb.vazhipadu.model.Offering;
import com.tdb.vazhipadu.model.Order;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    private static final String REPORT = "/api/temples/{templeId}/reports/daily-collection";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OfferingRepository offeringRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void reportGivesAmountAndCountPerOfferingToThePaisa() throws Exception {
        Offering pushpanjali = offering("30.10");
        Offering archana = offering("0.07");
        save(order("2020-01-01T04:00:00Z", pushpanjali, 2, archana, 3));
        save(order("2020-01-01T10:00:00Z", pushpanjali, 1));

        report("2020-01-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templeId").value(1))
                .andExpect(jsonPath("$.date").value("2020-01-01"))
                .andExpect(content().string(containsString("\"totalAmount\":90.51,")))
                .andExpect(jsonPath("$.offerings.length()").value(2))
                .andExpect(jsonPath("$.offerings[0].offeringId").value(pushpanjali.getId()))
                .andExpect(jsonPath("$.offerings[0].offeringName").value(pushpanjali.getName()))
                .andExpect(jsonPath("$.offerings[0].quantity").value(3))
                .andExpect(jsonPath("$.offerings[0].orderCount").value(2))
                .andExpect(content().string(containsString("\"totalAmount\":90.30}")))
                .andExpect(jsonPath("$.offerings[1].offeringId").value(archana.getId()))
                .andExpect(jsonPath("$.offerings[1].quantity").value(3))
                .andExpect(jsonPath("$.offerings[1].orderCount").value(1))
                .andExpect(content().string(containsString("\"totalAmount\":0.21}")));
    }

    @Test
    void orderPlacedAtQuarterPastMidnightIstCountsOnThatDate() throws Exception {
        save(order("2020-02-01T18:45:00Z", offering("100.00"), 1));

        report("2020-02-02").andExpect(jsonPath("$.offerings[0].quantity").value(1));
        report("2020-02-01").andExpect(jsonPath("$.offerings").isEmpty());
    }

    @Test
    void dayRunsFromIstMidnightInclusiveToNextIstMidnightExclusive() throws Exception {
        Offering pooja = offering("100.00");
        save(order("2020-03-01T18:29:59Z", pooja, 1));
        save(order("2020-03-01T18:30:00Z", pooja, 2));

        report("2020-03-01").andExpect(jsonPath("$.offerings[0].quantity").value(1));
        report("2020-03-02").andExpect(jsonPath("$.offerings[0].quantity").value(2));
    }

    @Test
    void cancelledOrdersAreNotCounted() throws Exception {
        Offering pooja = offering("100.00");
        save(order("2020-04-01T04:00:00Z", pooja, 1));
        Order cancelled = order("2020-04-01T05:00:00Z", pooja, 4);
        cancelled.cancel();
        save(cancelled);

        report("2020-04-01")
                .andExpect(jsonPath("$.offerings[0].quantity").value(1))
                .andExpect(jsonPath("$.offerings[0].orderCount").value(1))
                .andExpect(content().string(containsString("\"totalAmount\":100.00,")));
    }

    @Test
    void reportTotalEqualsSumOfOfferingTotals() throws Exception {
        save(order("2020-05-01T04:00:00Z", offering("12.34"), 7, offering("0.01"), 99));

        JsonNode report = objectMapper.readTree(report("2020-05-01").andReturn().getResponse().getContentAsString());

        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode offering : report.get("offerings")) {
            sum = sum.add(offering.get("totalAmount").decimalValue());
        }
        assertThat(sum).isEqualByComparingTo("87.37");
        assertThat(report.get("totalAmount").decimalValue()).isEqualByComparingTo(sum);
    }

    @Test
    void dayWithoutOrdersHasZeroTotal() throws Exception {
        report("2000-01-02")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalAmount\":0.00,")))
                .andExpect(jsonPath("$.offerings").isEmpty());
    }

    @Test
    void reportReturns404ForUnknownTemple() throws Exception {
        mockMvc.perform(get(REPORT, 999).param("date", "2020-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Temple not found with id: 999"));
    }

    @Test
    void reportReturns400ForFarFutureDate() throws Exception {
        report("+999999999-12-31")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date must be between 2000 and 9999"));
    }

    @Test
    void reportReturns400WhenDateMissing() throws Exception {
        mockMvc.perform(get(REPORT, 1)).andExpect(status().isBadRequest());
    }

    @Test
    void reportReturns400ForInvalidDate() throws Exception {
        mockMvc.perform(get(REPORT, 1).param("date", "01-12-2020")).andExpect(status().isBadRequest());
    }

    private ResultActions report(String date) throws Exception {
        return mockMvc.perform(get(REPORT, 1).param("date", date));
    }

    private Offering offering(String price) {
        return offeringRepository.save(
                new Offering(1L, "Pooja-" + UUID.randomUUID(), new BigDecimal(price), true, null));
    }

    private static Order order(String placedAt, Offering offering, int quantity) {
        Order order = new Order(1L, "Lakshmi", "9876543210", Nakshatra.ROHINI, LocalDate.of(2020, 1, 1),
                Instant.parse(placedAt));
        order.addLine(offering, quantity);
        return order;
    }

    private static Order order(String placedAt, Offering first, int firstQuantity, Offering second,
                               int secondQuantity) {
        Order order = order(placedAt, first, firstQuantity);
        order.addLine(second, secondQuantity);
        return order;
    }

    private void save(Order order) {
        orderRepository.save(order);
    }
}
