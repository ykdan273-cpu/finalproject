package com.example.vacancyscraper.core.analytics;

import java.util.List;

public interface VacancyDataPort {
    List<JobListing> fetch(AnalyticsQuery query);
}
