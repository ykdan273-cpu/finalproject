package com.example.vacancyscraper.core.analytics;

import com.example.vacancyscraper.model.VacancySource;

import java.util.Optional;

public record JobListing(
        String title,
        String city,
        VacancySource source,
        Integer salaryFrom,
        Integer salaryTo
) {
    public Optional<Integer> representativeSalary() {
        if (salaryFrom == null && salaryTo == null) {
            return Optional.empty();
        }
        if (salaryFrom != null && salaryTo != null) {
            return Optional.of((salaryFrom + salaryTo) / 2);
        }
        return Optional.ofNullable(salaryFrom != null ? salaryFrom : salaryTo);
    }
}
