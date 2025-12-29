package com.example.vacancyscraper.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalaryParserTest {

    @Test
    void parsesRange() {
        SalaryRange range = SalaryParser.parse("от 120 000 до 180 000 руб.");
        assertThat(range.from()).isEqualTo(120_000);
        assertThat(range.to()).isEqualTo(180_000);
    }

    @Test
    void parsesUpperBound() {
        SalaryRange range = SalaryParser.parse("до 90 000");
        assertThat(range.from()).isNull();
        assertThat(range.to()).isEqualTo(90_000);
    }

    @Test
    void parsesSingleValue() {
        SalaryRange range = SalaryParser.parse("200000");
        assertThat(range.from()).isEqualTo(200_000);
        assertThat(range.to()).isNull();
    }

    @Test
    void returnsEmptyWhenNoNumbers() {
        SalaryRange range = SalaryParser.parse("нет данных");
        assertThat(range.isEmpty()).isTrue();
    }
}
