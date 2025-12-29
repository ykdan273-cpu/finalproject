package com.example.vacancyscraper.core.analytics;

import com.example.vacancyscraper.model.VacancySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SalaryAnalyticsServiceTest {

    private final StubVacancyPort vacancyDataPort = new StubVacancyPort();
    private final SalaryAnalyticsService service = new SalaryAnalyticsService(vacancyDataPort);

    @Test
    void calculatesAggregatesWithSalaries() {
        AnalyticsQuery query = new AnalyticsQuery("moscow", VacancySource.HH);
        vacancyDataPort.data = List.of(
                new JobListing("Backend", "Moscow", VacancySource.HH, 100_000, 150_000),
                new JobListing("QA", "Moscow", VacancySource.HH, null, 90_000),
                new JobListing("Frontend", "Moscow", VacancySource.HH, 120_000, null),
                new JobListing("NoSalary", "Moscow", VacancySource.HH, null, null)
        );

        SalaryAnalyticsResult result = service.calculate(query);

        assertThat(result.totalVacancies()).isEqualTo(4);
        assertThat(result.vacanciesWithSalary()).isEqualTo(3);
        assertThat(result.minSalary()).isEqualTo(90_000);
        assertThat(result.maxSalary()).isEqualTo(125_000);
        assertThat(result.averageSalary()).isCloseTo(111_666.7, within(1.0));
        assertThat(result.city()).isEqualTo("moscow");
        assertThat(result.source()).isEqualTo(VacancySource.HH);
    }

    @Test
    void returnsZerosWhenNoSalaries() {
        AnalyticsQuery query = new AnalyticsQuery(null, null);
        vacancyDataPort.data = List.of(
                new JobListing("Backend", "Moscow", VacancySource.HH, null, null)
        );

        SalaryAnalyticsResult result = service.calculate(query);

        assertThat(result.vacanciesWithSalary()).isZero();
        assertThat(result.minSalary()).isZero();
        assertThat(result.maxSalary()).isZero();
        assertThat(result.averageSalary()).isZero();
    }

    @Test
    void usesPresenter() {
        AnalyticsQuery query = new AnalyticsQuery(null, null);
        vacancyDataPort.data = List.of(
                new JobListing("Backend", "Moscow", VacancySource.HH, 10_000, 20_000)
        );

        String rendered = service.calculate(query, result ->
                "avg=" + Math.round(result.averageSalary()) + ";total=" + result.totalVacancies());

        assertThat(rendered).isEqualTo("avg=15000;total=1");
    }

    private static final class StubVacancyPort implements VacancyDataPort {
        List<JobListing> data = List.of();

        @Override
        public List<JobListing> fetch(AnalyticsQuery query) {
            return data;
        }
    }
}
