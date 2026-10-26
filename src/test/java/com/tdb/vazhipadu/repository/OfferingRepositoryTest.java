package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.tdb.vazhipadu.model.Offering;

@DataJpaTest
class OfferingRepositoryTest {

    @Autowired
    private OfferingRepository offeringRepository;

    @Test
    void databaseRejectsSameNameInDifferentCaseForOneTemple() {
        offeringRepository.saveAndFlush(new Offering(1L, "Archana", new BigDecimal("50.00"), true, null));

        assertThatThrownBy(() ->
                offeringRepository.saveAndFlush(new Offering(1L, "archana", new BigDecimal("60.00"), true, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
