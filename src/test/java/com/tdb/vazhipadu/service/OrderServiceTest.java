package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.tdb.vazhipadu.constant.Nakshatra;
import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.constant.TimeZones;
import com.tdb.vazhipadu.contract.request.OrderItemRequest;
import com.tdb.vazhipadu.contract.request.OrderRequest;
import com.tdb.vazhipadu.contract.response.OrderResponse;
import com.tdb.vazhipadu.contract.response.OrderSummaryResponse;
import com.tdb.vazhipadu.contract.response.PageResponse;
import com.tdb.vazhipadu.exception.BadRequestException;
import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.exception.UnprocessableEntityException;
import com.tdb.vazhipadu.model.Offering;
import com.tdb.vazhipadu.model.Order;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long TEMPLE_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-10-26T04:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OfferingRepository offeringRepository;
    @Mock
    private TempleGuard templeGuard;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, TimeZones.IST);
        orderService = new OrderService(orderRepository, offeringRepository, templeGuard, clock);
    }

    @Test
    void placeCalculatesTotalFromOfferingPrices() {
        Offering pushpanjali = offering(10L, TEMPLE_ID, "Pushpanjali", "30.00", true);
        Offering archana = offering(11L, TEMPLE_ID, "Archana", "50.00", true);
        when(offeringRepository.findAllById(Set.of(10L, 11L))).thenReturn(List.of(pushpanjali, archana));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.place(TEMPLE_ID,
                request(TODAY, new OrderItemRequest(10L, 2), new OrderItemRequest(11L, 1)));

        assertThat(response.totalAmount()).isEqualByComparingTo("110.00");
        assertThat(response.status()).isEqualTo(OrderStatus.BOOKED);
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.devoteeName()).isEqualTo("Lakshmi");
        assertThat(response.items()).extracting("offeringName", "quantity")
                .containsExactly(tuple("Pushpanjali", 2),
                        tuple("Archana", 1));
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void placeRejectsInactiveOffering() {
        when(offeringRepository.findAllById(Set.of(10L)))
                .thenReturn(List.of(offering(10L, TEMPLE_ID, "Archana", "50.00", false)));

        assertThatThrownBy(() -> orderService.place(TEMPLE_ID, request(TODAY, new OrderItemRequest(10L, 1))))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Offering 'Archana' is not active");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeRejectsUnknownOffering() {
        when(offeringRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.place(TEMPLE_ID, request(TODAY, new OrderItemRequest(99L, 1))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Offering not found with id: 99");
    }

    @Test
    void placeRejectsOfferingOfAnotherTemple() {
        when(offeringRepository.findAllById(Set.of(10L)))
                .thenReturn(List.of(offering(10L, 2L, "Archana", "50.00", true)));

        assertThatThrownBy(() -> orderService.place(TEMPLE_ID, request(TODAY, new OrderItemRequest(10L, 1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void placeRejectsDuplicateOfferingInItems() {
        assertThatThrownBy(() -> orderService.place(TEMPLE_ID,
                request(TODAY, new OrderItemRequest(10L, 1), new OrderItemRequest(10L, 2))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Offering 10 appears more than once");
    }

    @Test
    void placeRejectsPastBookingDateInIst() {
        assertThatThrownBy(() -> orderService.place(TEMPLE_ID,
                request(TODAY.minusDays(1), new OrderItemRequest(10L, 1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Booking date must be today or later (IST)");
    }

    @Test
    void placeRejectsUnknownTemple() {
        doThrow(NotFoundException.of("Temple", 9L)).when(templeGuard).requireExists(9L);

        assertThatThrownBy(() -> orderService.place(9L, request(TODAY, new OrderItemRequest(10L, 1))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Temple not found with id: 9");
    }

    @Test
    void getReturnsOrder() {
        Order order = new Order(TEMPLE_ID, "Lakshmi", "9876543210", Nakshatra.ROHINI, TODAY, NOW);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThat(orderService.get(5L).devoteeName()).isEqualTo("Lakshmi");
    }

    @Test
    void getThrowsWhenOrderMissing() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found with id: 5");
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsPageSortedById() {
        Order order = new Order(TEMPLE_ID, "Lakshmi", "9876543210", Nakshatra.ROHINI, TODAY, NOW);
        Pageable expected = PageRequest.of(1, 2, Sort.by("id"));
        when(orderRepository.findAll(any(Specification.class), eq(expected)))
                .thenReturn(new PageImpl<>(List.of(order), expected, 3));

        PageResponse<OrderSummaryResponse> page =
                orderService.search(TEMPLE_ID, TODAY, OrderStatus.BOOKED, "9876543210", 1, 2);

        assertThat(page.content()).extracting(OrderSummaryResponse::phone).containsExactly("9876543210");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void searchRejectsUnknownTemple() {
        doThrow(NotFoundException.of("Temple", 9L)).when(templeGuard).requireExists(9L);

        assertThatThrownBy(() -> orderService.search(9L, null, null, null, 0, 20))
                .isInstanceOf(NotFoundException.class);
    }

    private static OrderRequest request(LocalDate date, OrderItemRequest... items) {
        return new OrderRequest(" Lakshmi ", "9876543210", Nakshatra.ROHINI, date, List.of(items));
    }

    private static Offering offering(Long id, Long templeId, String name, String price, boolean active) {
        Offering offering = new Offering(templeId, name, new BigDecimal(price), active);
        ReflectionTestUtils.setField(offering, "id", id);
        return offering;
    }
}
