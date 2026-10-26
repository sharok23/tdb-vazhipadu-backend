package com.tdb.vazhipadu.repository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.model.Order;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> search(Long templeId, LocalDate bookingDate, OrderStatus status,
                                              String phone) {
        Specification<Order> ofTemple = (root, query, cb) -> cb.equal(root.get("templeId"), templeId);
        return Specification.allOf(Stream.of(
                        ofTemple,
                        equalIfPresent("bookingDate", bookingDate),
                        equalIfPresent("status", status),
                        equalIfPresent("phone", phone))
                .filter(Objects::nonNull)
                .toList());
    }

    private static Specification<Order> equalIfPresent(String attribute, Object value) {
        return value == null ? null : (root, query, cb) -> cb.equal(root.get(attribute), value);
    }
}
