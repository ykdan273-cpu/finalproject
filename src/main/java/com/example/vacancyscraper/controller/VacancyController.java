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
@Tag(name = "Vacancy Parser API")
public class VacancyController {

    private final VacancyService vacancyService;

    public VacancyController(VacancyService vacancyService) {
        this.vacancyService = vacancyService;
    }

    @Operation(
            summary = "Распарсить одну вакансию",
            description = "Парсит вакансию по переданной ссылке (hh.ru, superjob.ru, career.habr.com, rabota.ru) и сохраняет в хранилище."
    )
    @ApiResponse(responseCode = "200", description = "Успешно распарсено",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse")
    public ResponseEntity<Vacancy> parseSingle(@Valid @RequestBody ParseRequest request) {
        Vacancy vacancy = vacancyService.parseSingle(request.getUrl());
        return ResponseEntity.ok(vacancy);
    }

    @Operation(
        summary = "Распарсить несколько вакансий",
        description = "Принимает список ссылок на вакансии и парсит их параллельно с помощью пула потоков."
    )
    @ApiResponse(responseCode = "200", description = "Успешно распарсено",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse-batch")
    public ResponseEntity<List<Vacancy>> parseBatch(@Valid @RequestBody ParseBatchRequest request) {
        List<Vacancy> vacancies = vacancyService.parseBatch(request.getUrls());
        return ResponseEntity.ok(vacancies);
    }

    @Operation(
            summary = "Список вакансий",
            description = "Возвращает список вакансий с фильтрацией по городу и источнику, сортировкой и выбором параллельной обработки."
    )
    @GetMapping("/answer")
    public ResponseEntity<List<Vacancy>> findAll(
            @Parameter(description = "Фильтр по названию города (частичное совпадение).")
            @RequestParam(name = "city", required = false) String city,
            @Parameter(description = "Источник вакансий: HH, SUPERJOB, HABR, RABOTA.")
            @RequestParam(name = "source", required = false) VacancySource source,
            @Parameter(description = "Сортировка: title (по умолчанию), date, salary.")
            @RequestParam(name = "sortBy", required = false, defaultValue = "title") String sortBy,
            @Parameter(description = "Использовать parallelStream для фильтрации.")
            @RequestParam(name = "parallel", required = false, defaultValue = "false") boolean parallel
    ) {
        List<Vacancy> vacancies = vacancyService.findAll(city, source, sortBy, parallel);
        return ResponseEntity.ok(vacancies);
    }

    @Operation(summary = "Статистика парсинга")
    @GetMapping("/stats")
    public ResponseEntity<ParsingStats> stats() {
        return ResponseEntity.ok(vacancyService.getStats());
    }
}
