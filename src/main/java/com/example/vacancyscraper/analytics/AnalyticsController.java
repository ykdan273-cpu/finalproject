package com.example.vacancyscraper.analytics;

import com.example.vacancyscraper.core.analytics.AnalyticsQuery;
import com.example.vacancyscraper.core.analytics.AnalyticsResultPort;
import com.example.vacancyscraper.core.analytics.SalaryAnalyticsUseCase;
import com.example.vacancyscraper.model.VacancySource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics", description = "Чистое ядро для расчета средней зарплаты с двумя адаптерами (REST и gRPC)")
public class AnalyticsController {

    private final SalaryAnalyticsUseCase salaryAnalyticsUseCase;
    private final AnalyticsResultPort<AnalyticsResponse> presenter;

    public AnalyticsController(SalaryAnalyticsUseCase salaryAnalyticsUseCase,
                               @Qualifier("restAnalyticsPresenter") AnalyticsResultPort<AnalyticsResponse> presenter) {
        this.salaryAnalyticsUseCase = salaryAnalyticsUseCase;
        this.presenter = presenter;
    }

    @Operation(summary = "Рассчитать агрегаты зарплат по вакансиям")
    @GetMapping("/salary")
    public AnalyticsResponse calculate(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) VacancySource source
    ) {
        AnalyticsQuery query = new AnalyticsQuery(city, source);
        return salaryAnalyticsUseCase.calculate(query, presenter);
    }
}
