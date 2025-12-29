package com.example.vacancyscraper.core.analytics;

import com.example.vacancyscraper.model.VacancySource;

public record AnalyticsQuery(String city, VacancySource source) {
}
