package com.example.vacancyscraper.controller;

import com.example.vacancyscraper.dto.ParseBatchRequest;
import com.example.vacancyscraper.dto.ParseRequest;
import com.example.vacancyscraper.model.ParsingStats;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.example.vacancyscraper.service.VacancyService;
import io.swagger.v3.oas.annotations.Operation;
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
            summary = "Парсинг одной ссылки",
            description = "Вставьте ссылку на страницу вакансии (hh.ru, superjob.ru, career.habr.com, rabota.ru) для одиночного парсинга"
    )
    @ApiResponse(responseCode = "200", description = "Ваш результат",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse")
    public ResponseEntity<Vacancy> parseSingle(@Valid @RequestBody ParseRequest request) {
        Vacancy vacancy = vacancyService.parseSingle(request.getUrl());
        return ResponseEntity.ok(vacancy);
    }

    @Operation(
            summary = "парсинг ссылок",
            description = "Список ссылок на страницы вакансий. Все ссылки будут обрабатываться параллельно через ExecutorService"
    )
    @ApiResponse(responseCode = "200", description = "описание вакнсий",
            content = @Content(schema = @Schema(implementation = Vacancy.class)))
    @PostMapping("/parse-batch")
    public ResponseEntity<List<Vacancy>> parseBatch(@Valid @RequestBody ParseBatchRequest request) {
        List<Vacancy> vacancies = vacancyService.parseBatch(request.getUrls());
        return ResponseEntity.ok(vacancies);
    }

    @Operation(summary = "Список  вакансий")
    @GetMapping("/answer")
    public ResponseEntity<List<Vacancy>> findAll(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) VacancySource source,
            @RequestParam(required = false, defaultValue = "title") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean parallel
    ) {
        List<Vacancy> vacancies = vacancyService.findAll(city, source, sortBy, parallel);
        return ResponseEntity.ok(vacancies);
    }

    @Operation(summary = "статистика")
    @GetMapping("/stats")
    public ResponseEntity<ParsingStats> stats() {
        return ResponseEntity.ok(vacancyService.getStats());
    }
}
