package com.tdb.vazhipadu.contract.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.tdb.vazhipadu.constant.Nakshatra;
import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.model.Order;

public record OrderResponse(
        Long id,
        Long templeId,
        String devoteeName,
        String phone,
        Nakshatra nakshatra,
        LocalDate bookingDate,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderLineResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTempleId(), order.getDevoteeName(), order.getPhone(),
                order.getNakshatra(), order.getBookingDate(), order.getStatus(), order.getTotalAmount(),
                order.getCreatedAt(), order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
