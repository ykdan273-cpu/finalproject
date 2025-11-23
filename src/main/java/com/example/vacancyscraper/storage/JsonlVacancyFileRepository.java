package com.example.vacancyscraper.storage;

import com.example.vacancyscraper.model.Vacancy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class JsonlVacancyFileRepository implements VacancyFileRepository {

    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final ReentrantLock fileLock = new ReentrantLock();

    public JsonlVacancyFileRepository(ObjectMapper objectMapper,
                                      @Value("${vacancy.storage-path}") String storagePath) {
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(storagePath);
    }

    @PostConstruct
    public void init() throws IOException {
        if (storagePath.getParent() != null) {
            Files.createDirectories(storagePath.getParent());
        }
        if (!Files.exists(storagePath)) {
            Files.createFile(storagePath);
        }
    }

    @Override
    public void save(Vacancy vacancy) {
        fileLock.lock();
        try {
            String json = objectMapper.writeValueAsString(vacancy);
            Files.writeString(storagePath, json + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write vacancy to file", e);
        } finally {
            fileLock.unlock();
        }
    }

    @Override
    public List<Vacancy> findAll() {
        fileLock.lock();
        try {
            List<Vacancy> vacancies = new ArrayList<>();
            if (!Files.exists(storagePath)) {
                return vacancies;
            }
            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                vacancies.add(objectMapper.readValue(line, Vacancy.class));
            }
            return vacancies;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read vacancies file", e);
        } finally {
            fileLock.unlock();
        }
    }
}
