package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tdb.vazhipadu.constant.OrderStatus;
import com.tdb.vazhipadu.contract.response.DailyCollectionResponse;
import com.tdb.vazhipadu.contract.response.OfferingCollectionResponse;
import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class DailyCollectionServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);
    private static final Instant IST_MIDNIGHT = Instant.parse("2026-11-30T18:30:00Z");
    private static final Instant NEXT_IST_MIDNIGHT = Instant.parse("2026-12-01T18:30:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TempleGuard templeGuard;
    @InjectMocks
    private DailyCollectionService service;

    @Test
    void reportSumsBookedOrdersPlacedBetweenIstMidnights() {
        when(orderRepository.collectionByOffering(1L, OrderStatus.BOOKED, IST_MIDNIGHT, NEXT_IST_MIDNIGHT))
                .thenReturn(List.of(
                        new OfferingCollectionResponse(10L, "Pushpanjali", 3L, 2L, new BigDecimal("90.30")),
                        new OfferingCollectionResponse(11L, "Archana", 3L, 1L, new BigDecimal("0.21"))));

        DailyCollectionResponse report = service.report(1L, DATE);

        assertThat(report.templeId()).isEqualTo(1L);
        assertThat(report.date()).isEqualTo(DATE);
        assertThat(report.totalAmount()).isEqualTo(new BigDecimal("90.51"));
        assertThat(report.offerings()).hasSize(2);
    }

    @Test
    void reportForDayWithoutOrdersIsZeroToThePaisa() {
        DailyCollectionResponse report = service.report(1L, DATE);

        assertThat(report.totalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(report.offerings()).isEmpty();
    }

    @Test
    void reportThrowsNotFoundForUnknownTemple() {
        doThrow(NotFoundException.of("Temple", 9L)).when(templeGuard).requireExists(9L);

        assertThatThrownBy(() -> service.report(9L, DATE)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(orderRepository);
    }
}
