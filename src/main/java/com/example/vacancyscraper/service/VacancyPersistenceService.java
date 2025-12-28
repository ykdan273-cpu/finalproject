package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.storage.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class VacancyPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(VacancyPersistenceService.class);

    private final VacancyRepository vacancyRepository;

    public VacancyPersistenceService(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = "vacancies", allEntries = true)
    public Vacancy saveWithLock(Vacancy vacancy) {
        Objects.requireNonNull(vacancy.getUrl(), "Vacancy url must not be null");
        return vacancyRepository.findByUrlForUpdate(vacancy.getUrl())
                .map(existing -> updateExisting(existing, vacancy))
                .orElseGet(() -> {
                    Vacancy saved = vacancyRepository.save(vacancy);
                    log.debug("Inserted new vacancy for url {}", vacancy.getUrl());
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public List<Vacancy> findAll() {
        return vacancyRepository.findAll();
    }

    private Vacancy updateExisting(Vacancy existing, Vacancy incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setCompany(incoming.getCompany());
        existing.setSalary(incoming.getSalary());
        existing.setRequirements(incoming.getRequirements());
        existing.setCity(incoming.getCity());
        existing.setPublishedDate(incoming.getPublishedDate());
        existing.setSource(incoming.getSource());
        existing.setUrl(incoming.getUrl());
        log.debug("Updated existing vacancy for url {}", incoming.getUrl());
        return vacancyRepository.save(existing);
    }
}
