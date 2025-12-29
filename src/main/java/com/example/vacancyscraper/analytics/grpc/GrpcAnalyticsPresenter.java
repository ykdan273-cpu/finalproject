package com.example.vacancyscraper.analytics.grpc;

import com.example.vacancyscraper.core.analytics.AnalyticsResultPort;
import com.example.vacancyscraper.core.analytics.SalaryAnalyticsResult;
import com.example.vacancyscraper.grpc.SalaryAnalyticsResponse;
import com.example.vacancyscraper.grpc.VacancySourceProto;
import com.example.vacancyscraper.model.VacancySource;
import org.springframework.stereotype.Component;

@Component("grpcAnalyticsPresenter")
public class GrpcAnalyticsPresenter implements AnalyticsResultPort<SalaryAnalyticsResponse> {
    @Override
    public SalaryAnalyticsResponse present(SalaryAnalyticsResult result) {
        return SalaryAnalyticsResponse.newBuilder()
                .setTotalVacancies(result.totalVacancies())
                .setVacanciesWithSalary(result.vacanciesWithSalary())
                .setMinSalary(result.minSalary())
                .setMaxSalary(result.maxSalary())
                .setAverageSalary(result.averageSalary())
                .setCity(result.city() == null ? "" : result.city())
                .setSource(mapSource(result.source()))
                .build();
    }

    private VacancySourceProto mapSource(VacancySource source) {
        if (source == null) {
            return VacancySourceProto.SOURCE_UNSPECIFIED;
        }
        return switch (source) {
            case HH -> VacancySourceProto.HH;
            case SUPERJOB -> VacancySourceProto.SUPERJOB;
            case HABR -> VacancySourceProto.HABR;
            case RABOTA -> VacancySourceProto.RABOTA;
        };
    }
}
