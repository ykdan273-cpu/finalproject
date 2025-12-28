package com.example.vacancyscraper.controller;

import com.example.vacancyscraper.dto.ParseBatchRequest;
import com.example.vacancyscraper.dto.ParseRequest;
import com.example.vacancyscraper.model.ParsingStats;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.example.vacancyscraper.service.VacancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping
@Tag(name = "API парсера вакансий")
public class VacancyController {

    private final VacancyService vacancyService;

    public VacancyController(VacancyService vacancyService) {
        this.vacancyService = vacancyService;
    }

    @Operation(
            summary = "Распарсить одну вакансию по URL",
            description = "Загружает страницу вакансии (hh.ru, superjob.ru, career.habr.com, rabota.ru) и возвращает распарсенный объект."
    )
    @ApiResponse(responseCode = "200", description = "Успешно распарсенная вакансия",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse")
    public ResponseEntity<Vacancy> parseSingle(@Valid @RequestBody ParseRequest request) {
        Vacancy vacancy = vacancyService.parseSingle(request.getUrl());
        return ResponseEntity.ok(vacancy);
    }

    @Operation(
        summary = "Распарсить несколько вакансий",
        description = "Принимает список URL, парсит каждую вакансию и возвращает результаты. Ошибки по отдельным ссылкам логируются."
    )
    @ApiResponse(responseCode = "200", description = "Успешно распарсенные вакансии",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse-batch")
    public ResponseEntity<List<Vacancy>> parseBatch(@Valid @RequestBody ParseBatchRequest request) {
        List<Vacancy> vacancies = vacancyService.parseBatch(request.getUrls());
        return ResponseEntity.ok(vacancies);
    }

    @Operation(
            summary = "Получить все сохранённые вакансии",
            description = "Фильтрация по городу и источнику, сортировка по title/date/salary, опционально parallelStream."
    )
    @GetMapping("/answer")
    public ResponseEntity<List<Vacancy>> findAll(
            @Parameter(description = "Фильтр по городу (подстрока, регистр игнорируется).")
            @RequestParam(name = "city", required = false) String city,
            @Parameter(description = "Источник вакансии: HH, SUPERJOB, HABR, RABOTA.")
            @RequestParam(name = "source", required = false) VacancySource source,
            @Parameter(description = "Сортировка: title, date, salary.")
            @RequestParam(name = "sortBy", required = false, defaultValue = "title") String sortBy,
            @Parameter(description = "Использовать parallelStream для сортировки/фильтрации.")
            @RequestParam(name = "parallel", required = false, defaultValue = "false") boolean parallel
    ) {
        List<Vacancy> vacancies = vacancyService.findAll(city, source, sortBy, parallel);
        return ResponseEntity.ok(vacancies);
    }

    @Operation(summary = "Получить статистику парсинга")
    @GetMapping("/stats")
    public ResponseEntity<ParsingStats> stats() {
        return ResponseEntity.ok(vacancyService.getStats());
    }
}
