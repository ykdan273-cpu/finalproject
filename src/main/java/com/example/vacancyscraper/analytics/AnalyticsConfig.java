package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.core.analytics.SalaryAnalyticsService;
import com.example.vacancyscraper.core.analytics.SalaryAnalyticsUseCase;
import com.example.vacancyscraper.core.analytics.VacancyDataPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsConfig {

    @Bean
    public SalaryAnalyticsUseCase salaryAnalyticsUseCase(VacancyDataPort vacancyDataPort) {
        return new SalaryAnalyticsService(vacancyDataPort);
    }
}
