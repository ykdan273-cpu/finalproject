package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonlVacancyFileRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndReadsVacancies() throws IOException {
        Path file = tempDir.resolve("vacancies.jsonl");
        JsonlVacancyFileRepository repository = new JsonlVacancyFileRepository(new ObjectMapper(), file.toString());
        repository.init();

        Vacancy vacancy = new Vacancy("Dev", "Acme", "100", "Java", "Москва",
                LocalDate.of(2024, 1, 1), VacancySource.HH, "https://hh.ru/vacancy/1");
        repository.save(vacancy);

        List<Vacancy> all = repository.findAll();

        assertEquals(1, all.size());
        assertEquals("Dev", all.get(0).getTitle());
        assertTrue(file.toFile().exists());
    }
}
