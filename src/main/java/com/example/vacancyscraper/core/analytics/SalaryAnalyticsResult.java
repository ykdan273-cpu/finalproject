package com.example.vacancyscraper.core.analytics;

import com.example.vacancyscraper.model.VacancySource;

public record SalaryAnalyticsResult(
        long totalVacancies,
        long vacanciesWithSalary,
        int minSalary,
        int maxSalary,
        double averageSalary,
        String city,
        VacancySource source
) {
}
