package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.tdb.vazhipadu.constant.Nakshatra;
import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.model.Order;

public record OrderSummaryResponse(
        Long id,
        String devoteeName,
        String phone,
        Nakshatra nakshatra,
        LocalDate bookingDate,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getDevoteeName(), order.getPhone(),
                order.getNakshatra(), order.getBookingDate(), order.getStatus(), order.getTotalAmount(),
                order.getCreatedAt());
    }
}
