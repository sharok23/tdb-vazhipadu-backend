package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tdb.vazhipadu.contract.request.OfferingRequest;
import com.tdb.vazhipadu.contract.response.OfferingResponse;
import com.tdb.vazhipadu.exception.ConflictException;
import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.model.Offering;
import com.tdb.vazhipadu.repository.OfferingRepository;

@ExtendWith(MockitoExtension.class)
class OfferingServiceTest {

    private static final Long TEMPLE_ID = 1L;

    @Mock
    private OfferingRepository offeringRepository;
    @Mock
    private TempleGuard templeGuard;
    @InjectMocks
    private OfferingService offeringService;

    @Test
    void createTrimsNameAndSaves() {
        when(offeringRepository.save(any(Offering.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferingResponse response = offeringService.create(TEMPLE_ID, request(" Archana "));

        assertThat(response.name()).isEqualTo("Archana");
        assertThat(response.templeId()).isEqualTo(TEMPLE_ID);
        assertThat(response.price()).isEqualByComparingTo("50.00");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateName() {
        when(offeringRepository.existsByTempleIdAndNameIgnoreCase(TEMPLE_ID, "Archana")).thenReturn(true);

        assertThatThrownBy(() -> offeringService.create(TEMPLE_ID, request("Archana")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Offering 'Archana' already exists for this temple");
        verify(offeringRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownTemple() {
        doThrow(NotFoundException.of("Temple", 9L)).when(templeGuard).requireExists(9L);

        assertThatThrownBy(() -> offeringService.create(9L, request("Archana")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listMapsOfferings() {
        when(offeringRepository.findByTempleIdOrderById(TEMPLE_ID))
                .thenReturn(List.of(new Offering(TEMPLE_ID, "Archana", new BigDecimal("50.00"), true, null)));

        assertThat(offeringService.list(TEMPLE_ID)).extracting(OfferingResponse::name).containsExactly("Archana");
    }

    @Test
    void updateChangesFields() {
        Offering offering = new Offering(TEMPLE_ID, "Archana", new BigDecimal("50.00"), true, null);
        when(offeringRepository.findByIdAndTempleId(3L, TEMPLE_ID)).thenReturn(Optional.of(offering));

        OfferingResponse response = offeringService.update(TEMPLE_ID, 3L,
                new OfferingRequest("Archana", new BigDecimal("60.00"), false, 50));

        assertThat(response.price()).isEqualByComparingTo("60.00");
        assertThat(response.active()).isFalse();
        assertThat(response.dailyLimit()).isEqualTo(50);
    }

    @Test
    void updateRejectsUnknownOffering() {
        when(offeringRepository.findByIdAndTempleId(3L, TEMPLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.update(TEMPLE_ID, 3L, request("Archana")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Offering not found with id: 3");
    }

    @Test
    void updateRejectsNameUsedByAnotherOffering() {
        Offering offering = new Offering(TEMPLE_ID, "Archana", new BigDecimal("50.00"), true, null);
        when(offeringRepository.findByIdAndTempleId(3L, TEMPLE_ID)).thenReturn(Optional.of(offering));
        when(offeringRepository.existsByTempleIdAndNameIgnoreCaseAndIdNot(TEMPLE_ID, "Pushpanjali", 3L))
                .thenReturn(true);

        assertThatThrownBy(() -> offeringService.update(TEMPLE_ID, 3L, request("Pushpanjali")))
                .isInstanceOf(ConflictException.class);
    }

    private static OfferingRequest request(String name) {
        return new OfferingRequest(name, new BigDecimal("50.00"), true, null);
    }
}
