package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void commonSearchesUseAnIndex(String filter, String index) {
        String plan = jdbcTemplate.queryForObject(
                "EXPLAIN SELECT id FROM orders WHERE temple_id = 1 AND " + filter + " ORDER BY id", String.class);

        assertThat(plan).containsIgnoringCase(index);
    }
}
