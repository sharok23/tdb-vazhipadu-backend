package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
class OrderLineIndexTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // H2 auto-indexes foreign keys and prefers those in plans; PostgreSQL does not, so check the migration itself
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "IDX_ORDER_LINE_OFFERING_ORDER | OFFERING_ID,ORDER_ID",
            "IDX_ORDER_LINE_ORDER          | ORDER_ID"
    })
    void orderLineForeignKeysAreIndexed(String index, String columns) {
        List<String> actual = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                WHERE TABLE_NAME = 'ORDER_LINE' AND INDEX_NAME = ?
                ORDER BY ORDINAL_POSITION
                """, String.class, index);

        assertThat(actual).containsExactly(columns.split(","));
    }
}
