package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;

import java.util.List;

/**
 * Abstraction over vacancy persistence so we can swap JPA (PostgreSQL/H2) and MongoDB backends.
 */
public interface VacancyStorage {

    /**
     * Inserts or updates vacancy by URL (unique business key).
     */
    Vacancy upsert(Vacancy vacancy);

    List<Vacancy> findAll();

    long count();

    /**
     * Human readable backend name for logs/metrics.
     */
    String backendName();
}
