package com.example.vacancyscraper.core.analytics;

public interface SalaryAnalyticsUseCase {
    SalaryAnalyticsResult calculate(AnalyticsQuery query);

    <T> T calculate(AnalyticsQuery query, AnalyticsResultPort<T> presenter);
}
