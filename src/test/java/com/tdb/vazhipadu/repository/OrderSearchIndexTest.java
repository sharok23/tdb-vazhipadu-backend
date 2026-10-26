package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
class OrderSearchIndexTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "booking_date = DATE '2026-12-01' | IDX_ORDERS_TEMPLE_DATE",
            "phone = '9876543210'             | IDX_ORDERS_TEMPLE_PHONE",
            "status = 'BOOKED'                | IDX_ORDERS_TEMPLE_STATUS"
    })
    void filteredSearchesUseTheirIndex(String filter, String index) {
        assertThat(plan(filter)).containsIgnoringCase(index);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 = 1", "booking_date = DATE '2026-12-01' AND status = 'BOOKED'"})
    void searchesNeverScanTheWholeTable(String filter) {
        assertThat(plan(filter)).doesNotContainIgnoringCase("tableScan");
    }

    private String plan(String filter) {
        return jdbcTemplate.queryForObject(
                "EXPLAIN SELECT id FROM orders WHERE temple_id = 1 AND " + filter + " ORDER BY id", String.class);
    }
}
