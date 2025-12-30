package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.metrics.ParsingMetrics;
import com.example.vacancyscraper.storage.VacancyStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class VacancyPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(VacancyPersistenceService.class);

    private final VacancyStorage vacancyStorage;
    private final ParsingMetrics parsingMetrics;

    public VacancyPersistenceService(VacancyStorage vacancyStorage, ParsingMetrics parsingMetrics) {
        this.vacancyStorage = vacancyStorage;
        this.parsingMetrics = parsingMetrics;
    }

    @CacheEvict(cacheNames = "vacancies", allEntries = true)
    public Vacancy saveWithLock(Vacancy vacancy) {
        Objects.requireNonNull(vacancy.getUrl(), "Vacancy url must not be null");
        Vacancy saved = vacancyStorage.upsert(vacancy);
        parsingMetrics.incrementSaved();
        log.debug("Persisted vacancy via {} backend for url {}", vacancyStorage.backendName(), vacancy.getUrl());
        return saved;
    }

    public List<Vacancy> findAll() {
        return vacancyStorage.findAll();
    }

    public long count() {
        return vacancyStorage.count();
    }

    public String backendName() {
        return vacancyStorage.backendName();
    }
}
