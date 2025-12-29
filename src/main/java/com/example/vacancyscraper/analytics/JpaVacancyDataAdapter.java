package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.core.analytics.AnalyticsQuery;
import com.example.vacancyscraper.core.analytics.JobListing;
import com.example.vacancyscraper.core.analytics.VacancyDataPort;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.example.vacancyscraper.service.VacancyPersistenceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "vacancy.analytics.source", havingValue = "jpa", matchIfMissing = true)
public class JpaVacancyDataAdapter implements VacancyDataPort {

    private final VacancyPersistenceService vacancyPersistenceService;

    public JpaVacancyDataAdapter(VacancyPersistenceService vacancyPersistenceService) {
        this.vacancyPersistenceService = vacancyPersistenceService;
    }

    @Override
    public List<JobListing> fetch(AnalyticsQuery query) {
        List<Vacancy> vacancies = vacancyPersistenceService.findAll();
        return vacancies.stream()
                .filter(v -> matchesCity(query.city(), v.getCity()))
                .filter(v -> matchesSource(query.source(), v.getSource()))
                .map(this::toJobListing)
                .collect(Collectors.toList());
    }

    private boolean matchesCity(String requestedCity, String actualCity) {
        if (!StringUtils.hasText(requestedCity)) {
            return true;
        }
        return actualCity != null &&
                actualCity.toLowerCase(Locale.ROOT).contains(requestedCity.toLowerCase(Locale.ROOT));
    }

    private boolean matchesSource(VacancySource requestedSource, VacancySource actualSource) {
        return requestedSource == null || requestedSource == actualSource;
    }

    private JobListing toJobListing(Vacancy vacancy) {
        SalaryRange range = SalaryParser.parse(vacancy.getSalary());
        Integer from = range.isEmpty() ? null : range.from();
        Integer to = range.isEmpty() ? null : range.to();
        return new JobListing(
                vacancy.getTitle(),
                vacancy.getCity(),
                vacancy.getSource(),
                from,
                to
        );
    }
}
