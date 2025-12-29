package com.example.vacancyscraper.core.analytics;

import java.util.List;

public class SalaryAnalyticsService implements SalaryAnalyticsUseCase {

    private final VacancyDataPort vacancyDataPort;

    public SalaryAnalyticsService(VacancyDataPort vacancyDataPort) {
        this.vacancyDataPort = vacancyDataPort;
    }

    @Override
    public SalaryAnalyticsResult calculate(AnalyticsQuery query) {
        return calculate(query, AnalyticsResultPort.identity());
    }

    @Override
    public <T> T calculate(AnalyticsQuery query, AnalyticsResultPort<T> presenter) {
        AnalyticsQuery safeQuery = query != null ? query : new AnalyticsQuery(null, null);
        List<JobListing> vacancies = vacancyDataPort.fetch(safeQuery);

        List<Integer> salaries = vacancies.stream()
                .flatMap(job -> job.representativeSalary().stream())
                .sorted()
                .toList();

        long withSalaryCount = salaries.size();
        int minSalary = salaries.isEmpty() ? 0 : salaries.get(0);
        int maxSalary = salaries.isEmpty() ? 0 : salaries.get(salaries.size() - 1);
        double avgSalary = salaries.isEmpty() ? 0.0 :
                salaries.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        SalaryAnalyticsResult result = new SalaryAnalyticsResult(
                vacancies.size(),
                withSalaryCount,
                minSalary,
                maxSalary,
                avgSalary,
                safeQuery.city(),
                safeQuery.source()
        );
        return presenter.present(result);
    }
}
