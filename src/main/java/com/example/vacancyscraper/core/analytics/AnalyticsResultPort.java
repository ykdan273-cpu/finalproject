package com.example.vacancyscraper.core.analytics;

public interface AnalyticsResultPort<T> {
    T present(SalaryAnalyticsResult result);

    static AnalyticsResultPort<SalaryAnalyticsResult> identity() {
        return result -> result;
    }
}
