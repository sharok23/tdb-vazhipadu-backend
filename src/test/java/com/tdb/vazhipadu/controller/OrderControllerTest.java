package com.tdb.vazhipadu.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.constant.TimeZones;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    private static final String ORDERS = "/api/temples/1/orders";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void placeOrderCalculatesTotalOnServer() throws Exception {
        long pushpanjali = createOffering("30.00", true);
        long archana = createOffering("50.00", true);
        String items = "[{\"offeringId\":%d,\"quantity\":2},{\"offeringId\":%d,\"quantity\":1}]"
                .formatted(pushpanjali, archana);

        String response = mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(tomorrow(), "9876543210", items)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(110.00))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.devoteeName").value("Lakshmi"))
                .andExpect(jsonPath("$.nakshatra").value("ROHINI"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(60.00))
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(response).get("id").asLong();
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.totalAmount").value(110.00))
                .andExpect(jsonPath("$.items[1].unitPrice").value(50.00));
    }

    @Test
    void placeOrderReturnsLocationHeader() throws Exception {
        long offering = createOffering("30.00", true);

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(today(), "9876543210", oneItem(offering, 1))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void placeOrderReturns422ForInactiveOffering() throws Exception {
        long inactive = createOffering("50.00", false);

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(tomorrow(), "9876543210", oneItem(inactive, 1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void placeOrderReturns404ForUnknownOffering() throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(tomorrow(), "9876543210", oneItem(999999, 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Offering not found with id: 999999"));
    }

    @Test
    void placeOrderReturns404ForUnknownTemple() throws Exception {
        long offering = createOffering("30.00", true);

        mockMvc.perform(post("/api/temples/999/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(order(tomorrow(), "9876543210", oneItem(offering, 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeOrderReturns400ForPastDate() throws Exception {
        long offering = createOffering("30.00", true);

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(today().minusDays(1), "9876543210", oneItem(offering, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking date must be today or later (IST)"));
    }

    @Test
    void placeOrderReturns400ForInvalidFields() throws Exception {
        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"devoteeName\":\"\",\"phone\":\"12345\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.devoteeName").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.nakshatra").exists())
                .andExpect(jsonPath("$.fieldErrors.bookingDate").exists())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    void placeOrderReturns400ForZeroQuantity() throws Exception {
        long offering = createOffering("30.00", true);

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON)
                        .content(order(tomorrow(), "9876543210", oneItem(offering, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items[0].quantity']").exists());
    }

    @Test
    void placeOrderReturns400ForUnknownNakshatra() throws Exception {
        String body = order(tomorrow(), "9876543210", oneItem(1, 1)).replace("ROHINI", "PLUTO");

        mockMvc.perform(post(ORDERS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
    }

    @Test
    void getOrderReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 999999"));
    }

    private long createOffering(String price, boolean active) throws Exception {
        String body = "{\"name\":\"Offering-%s\",\"price\":%s,\"active\":%s}"
                .formatted(UUID.randomUUID(), price, active);
        String response = mockMvc.perform(post("/api/temples/1/offerings")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private static String order(LocalDate date, String phone, String items) {
        return """
                {"devoteeName":" Lakshmi ","phone":"%s","nakshatra":"ROHINI","bookingDate":"%s","items":%s}
                """.formatted(phone, date, items);
    }

    private static String oneItem(long offeringId, int quantity) {
        return "[{\"offeringId\":%d,\"quantity\":%d}]".formatted(offeringId, quantity);
    }

    private static LocalDate today() {
        return LocalDate.now(TimeZones.IST);
    }

    private static LocalDate tomorrow() {
        return today().plusDays(1);
    }

    @Test
    void getOrderReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'orderId'"));
    }
}
