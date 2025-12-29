package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.core.analytics.AnalyticsQuery;
import com.example.vacancyscraper.core.analytics.JobListing;
import com.example.vacancyscraper.core.analytics.VacancyDataPort;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "vacancy.analytics.source", havingValue = "file")
public class FileVacancyDataAdapter implements VacancyDataPort {

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public FileVacancyDataAdapter(@Value("${vacancy.analytics.file:data/vacancies.jsonl}") String filePath,
                                  ObjectMapper objectMapper) {
        this.filePath = Path.of(filePath);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JobListing> fetch(AnalyticsQuery query) {
        if (!Files.exists(filePath)) {
            return List.of();
        }
        try (Stream<String> lines = Files.lines(filePath)) {
            return lines
                    .filter(StringUtils::hasText)
                    .map(this::safeParse)
                    .filter(Objects::nonNull)
                    .filter(v -> matchesCity(query.city(), v.getCity()))
                    .filter(v -> matchesSource(query.source(), v.getSource()))
                    .map(this::toJobListing)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read vacancies from file " + filePath, e);
        }
    }

    private Vacancy safeParse(String line) {
        try {
            return objectMapper.readValue(line, Vacancy.class);
        } catch (Exception ignored) {
            return null;
        }
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
