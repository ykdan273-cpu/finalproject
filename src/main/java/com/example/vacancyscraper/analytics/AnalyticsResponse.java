package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.model.VacancySource;

public record AnalyticsResponse(
        long totalVacancies,
        long vacanciesWithSalary,
        int minSalary,
        int maxSalary,
        double averageSalary,
        String city,
        VacancySource source
) {
}
