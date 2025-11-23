package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;

import java.util.List;

public interface VacancyFileRepository {
    void save(Vacancy vacancy);

    List<Vacancy> findAll();
}
