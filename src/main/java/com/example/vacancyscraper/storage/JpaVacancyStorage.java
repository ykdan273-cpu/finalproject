package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "vacancy.storage.backend", havingValue = "jpa", matchIfMissing = true)
public class JpaVacancyStorage implements VacancyStorage {

    private static final Logger log = LoggerFactory.getLogger(JpaVacancyStorage.class);

    private final VacancyRepository vacancyRepository;

    public JpaVacancyStorage(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    @Override
    @Transactional
    public Vacancy upsert(Vacancy vacancy) {
        Objects.requireNonNull(vacancy.getUrl(), "Vacancy url must not be null");
        return vacancyRepository.findByUrlForUpdate(vacancy.getUrl())
                .map(existing -> updateExisting(existing, vacancy))
                .orElseGet(() -> {
                    Vacancy saved = vacancyRepository.save(vacancy);
                    log.debug("Inserted new vacancy for url {}", vacancy.getUrl());
                    return saved;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vacancy> findAll() {
        return vacancyRepository.findAll();
    }

    @Override
    public long count() {
        return vacancyRepository.count();
    }

    @Override
    public String backendName() {
        return "postgresql/jpa";
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
