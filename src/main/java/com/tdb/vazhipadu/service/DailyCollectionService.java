package com.tdb.vazhipadu.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.constant.TimeZones;
import com.tdb.vazhipadu.contract.response.DailyCollectionResponse;
import com.tdb.vazhipadu.contract.response.OfferingCollectionResponse;
import com.tdb.vazhipadu.exception.BadRequestException;
import com.tdb.vazhipadu.repository.OrderRepository;

@Service
public class DailyCollectionService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 9999;

    private final OrderRepository orderRepository;
    private final TempleGuard templeGuard;

    public DailyCollectionService(OrderRepository orderRepository, TempleGuard templeGuard) {
        this.orderRepository = orderRepository;
        this.templeGuard = templeGuard;
    }

    @Transactional(readOnly = true)
    public DailyCollectionResponse report(Long templeId, LocalDate date) {
        templeGuard.requireExists(templeId);
        if (date.getYear() < MIN_YEAR || date.getYear() > MAX_YEAR) {
            throw new BadRequestException("Date must be between %d and %d".formatted(MIN_YEAR, MAX_YEAR));
        }
        Instant from = date.atStartOfDay(TimeZones.IST).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(TimeZones.IST).toInstant();
        List<OfferingCollectionResponse> offerings =
                orderRepository.collectionByOffering(templeId, OrderStatus.BOOKED, from, to);
        BigDecimal total = offerings.stream()
                .map(OfferingCollectionResponse::totalAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new DailyCollectionResponse(templeId, date, total, offerings);
    }
}
