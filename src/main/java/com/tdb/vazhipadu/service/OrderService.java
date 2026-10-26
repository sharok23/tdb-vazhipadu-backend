package com.tdb.vazhipadu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.contract.request.OrderItemRequest;
import com.tdb.vazhipadu.contract.request.OrderRequest;
import com.tdb.vazhipadu.contract.response.OrderResponse;
import com.tdb.vazhipadu.contract.response.OrderSummaryResponse;
import com.tdb.vazhipadu.contract.response.PageResponse;
import com.tdb.vazhipadu.exception.BadRequestException;
import com.tdb.vazhipadu.exception.ConflictException;
import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.exception.UnprocessableEntityException;
import com.tdb.vazhipadu.model.Offering;
import com.tdb.vazhipadu.model.Order;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;
import com.tdb.vazhipadu.repository.OrderSpecifications;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OfferingRepository offeringRepository;
    private final TempleGuard templeGuard;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, OfferingRepository offeringRepository,
                        TempleGuard templeGuard, Clock clock) {
        this.orderRepository = orderRepository;
        this.offeringRepository = offeringRepository;
        this.templeGuard = templeGuard;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse place(Long templeId, OrderRequest request) {
        templeGuard.requireExists(templeId);
        requireNotPast(request.bookingDate());
        Map<Long, Offering> offerings = loadBookableOfferings(templeId, request.items());
        request.items().forEach(item ->
                requireWithinDailyLimit(offerings.get(item.offeringId()), request.bookingDate(), item.quantity()));

        Order order = new Order(templeId, request.devoteeName().trim(), request.phone(), request.nakshatra(),
                request.bookingDate(), clock.instant());
        request.items().forEach(item -> order.addLine(offerings.get(item.offeringId()), item.quantity()));
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> NotFoundException.of("Order", orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> search(Long templeId, LocalDate bookingDate, OrderStatus status,
                                                     String phone, int page, int size) {
        templeGuard.requireExists(templeId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id"));
        return PageResponse.from(
                orderRepository.findAll(OrderSpecifications.search(templeId, bookingDate, status, phone), pageRequest),
                OrderSummaryResponse::from);
    }

    private void requireNotPast(LocalDate bookingDate) {
        if (bookingDate.isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("Booking date must be today or later (IST)");
        }
    }

    private void requireWithinDailyLimit(Offering offering, LocalDate bookingDate, int quantity) {
        Integer limit = offering.getDailyLimit();
        if (limit == null) {
            return;
        }
        long booked = orderRepository.sumQuantity(offering.getId(), bookingDate, OrderStatus.BOOKED);
        if (booked + quantity > limit) {
            throw new ConflictException("Daily limit reached for '%s' on %s: %d of %d left"
                    .formatted(offering.getName(), bookingDate, Math.max(0, limit - booked), limit));
        }
    }

    private Map<Long, Offering> loadBookableOfferings(Long templeId, List<OrderItemRequest> items) {
        Set<Long> ids = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!ids.add(item.offeringId())) {
                throw new BadRequestException("Offering %d appears more than once".formatted(item.offeringId()));
            }
        }
        Map<Long, Offering> offerings = offeringRepository.findAllByIdForUpdate(ids).stream()
                .filter(offering -> offering.getTempleId().equals(templeId))
                .collect(Collectors.toMap(Offering::getId, Function.identity()));
        for (Long id : ids) {
            Offering offering = offerings.get(id);
            if (offering == null) {
                throw NotFoundException.of("Offering", id);
            }
            if (!offering.isActive()) {
                throw new UnprocessableEntityException("Offering '%s' is not active".formatted(offering.getName()));
            }
        }
        return offerings;
    }
}
