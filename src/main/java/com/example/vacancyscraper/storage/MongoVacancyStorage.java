package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.storage.mongo.VacancyDocument;
import com.example.vacancyscraper.storage.mongo.VacancyMongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "vacancy.storage.backend", havingValue = "mongo")
public class MongoVacancyStorage implements VacancyStorage {

    private static final Logger log = LoggerFactory.getLogger(MongoVacancyStorage.class);

    private final VacancyMongoRepository vacancyMongoRepository;

    public MongoVacancyStorage(VacancyMongoRepository vacancyMongoRepository) {
        this.vacancyMongoRepository = vacancyMongoRepository;
    }

    @Override
    public Vacancy upsert(Vacancy vacancy) {
        Objects.requireNonNull(vacancy.getUrl(), "Vacancy url must not be null");
        VacancyDocument document = vacancyMongoRepository.findByUrl(vacancy.getUrl())
                .orElseGet(VacancyDocument::new);
        copyToDocument(vacancy, document);
        VacancyDocument saved = vacancyMongoRepository.save(document);
        log.debug("Upserted vacancy in MongoDB for url {}", vacancy.getUrl());
        return toDomain(saved);
    }

    @Override
    public List<Vacancy> findAll() {
        return vacancyMongoRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return vacancyMongoRepository.count();
    }

    @Override
    public String backendName() {
        return "mongodb";
    }

    private void copyToDocument(Vacancy vacancy, VacancyDocument document) {
        document.setTitle(vacancy.getTitle());
        document.setCompany(vacancy.getCompany());
        document.setSalary(vacancy.getSalary());
        document.setRequirements(vacancy.getRequirements());
        document.setCity(vacancy.getCity());
        document.setPublishedDate(vacancy.getPublishedDate());
        document.setSource(vacancy.getSource());
        document.setUrl(vacancy.getUrl());
    }

    private Vacancy toDomain(VacancyDocument document) {
        Vacancy vacancy = new Vacancy(
                document.getTitle(),
                document.getCompany(),
                document.getSalary(),
                document.getRequirements(),
                document.getCity(),
                document.getPublishedDate(),
                document.getSource(),
                document.getUrl()
        );
        vacancy.setId(parseId(document.getId()));
        return vacancy;
    }

    private Long parseId(String mongoId) {
        if (mongoId == null) {
            return null;
        }
        try {
            // Mongo uses hex string ids, but we only need a stable numeric placeholder for DTO compatibility
            return Long.parseLong(mongoId.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
