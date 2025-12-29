package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.Vacancy;
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

    public VacancyPersistenceService(VacancyStorage vacancyStorage) {
        this.vacancyStorage = vacancyStorage;
    }

    @CacheEvict(cacheNames = "vacancies", allEntries = true)
    public Vacancy saveWithLock(Vacancy vacancy) {
        Objects.requireNonNull(vacancy.getUrl(), "Vacancy url must not be null");
        Vacancy saved = vacancyStorage.upsert(vacancy);
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
