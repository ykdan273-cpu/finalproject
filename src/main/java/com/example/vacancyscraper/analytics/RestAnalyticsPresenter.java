package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.core.analytics.AnalyticsResultPort;
import com.example.vacancyscraper.core.analytics.SalaryAnalyticsResult;
import org.springframework.stereotype.Component;

@Component("restAnalyticsPresenter")
public class RestAnalyticsPresenter implements AnalyticsResultPort<AnalyticsResponse> {
    @Override
    public AnalyticsResponse present(SalaryAnalyticsResult result) {
        return new AnalyticsResponse(
                result.totalVacancies(),
                result.vacanciesWithSalary(),
                result.minSalary(),
                result.maxSalary(),
                result.averageSalary(),
                result.city(),
                result.source()
        );
    }
}
