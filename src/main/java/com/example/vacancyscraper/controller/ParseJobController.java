package com.example.vacancyscraper.controller;

import com.example.vacancyscraper.dto.ParseJobRequest;
import com.example.vacancyscraper.dto.ParseJobResponse;
import com.example.vacancyscraper.model.ParseJob;
import com.example.vacancyscraper.model.ParseJobStatus;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.service.ParseJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Асинхронные задачи парсинга")
public class ParseJobController {

    private final ParseJobService parseJobService;

    public ParseJobController(ParseJobService parseJobService) {
        this.parseJobService = parseJobService;
    }

    @Operation(summary = "Запустить асинхронную задачу парсинга (polling/WebSocket)")
    @PostMapping
    public ResponseEntity<ParseJobResponse> startJob(@Valid @RequestBody ParseJobRequest request) {
        ParseJob job = parseJobService.startJob(request.getUrl(), request.getDelayMs());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ParseJobResponse.from(job));
    }

    @Operation(summary = "Статус задачи (используется фронтом в режиме polling)")
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ParseJobResponse> status(@PathVariable String jobId) {
        return ResponseEntity.ok(ParseJobResponse.from(parseJobService.getJob(jobId)));
    }

    @Operation(summary = "Результат задачи (202, пока не завершена)")
    @GetMapping("/{jobId}/result")
    public ResponseEntity<?> result(@PathVariable String jobId) {
        ParseJob job = parseJobService.getJob(jobId);
        if (!job.isFinished()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ParseJobResponse.from(job));
        }
        if (job.getStatus() == ParseJobStatus.ERROR) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ParseJobResponse.from(job));
        }
        Vacancy vacancy = job.getVacancy();
        return ResponseEntity.ok(vacancy);
    }
}
