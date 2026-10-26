package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
class OrderCollectionIndexTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dailyCollectionUsesCreatedAtIndex() {
        String plan = jdbcTemplate.queryForObject("""
                EXPLAIN SELECT id FROM orders WHERE temple_id = 1
                AND created_at >= TIMESTAMP WITH TIME ZONE '2026-11-30 18:30:00Z'
                AND created_at < TIMESTAMP WITH TIME ZONE '2026-12-01 18:30:00Z'
                """, String.class);

        assertThat(plan).containsIgnoringCase("IDX_ORDERS_TEMPLE_CREATED");
    }
}
