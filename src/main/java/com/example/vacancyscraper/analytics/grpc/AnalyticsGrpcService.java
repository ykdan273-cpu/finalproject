package com.example.vacancyscraper.analytics.grpc;

import com.example.vacancyscraper.core.analytics.AnalyticsQuery;
import com.example.vacancyscraper.core.analytics.AnalyticsResultPort;
import com.example.vacancyscraper.core.analytics.SalaryAnalyticsUseCase;
import com.example.vacancyscraper.grpc.AnalyticsRequest;
import com.example.vacancyscraper.grpc.AnalyticsServiceGrpc;
import com.example.vacancyscraper.grpc.SalaryAnalyticsResponse;
import com.example.vacancyscraper.grpc.VacancySourceProto;
import com.example.vacancyscraper.model.VacancySource;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AnalyticsGrpcService extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsGrpcService.class);

    private final SalaryAnalyticsUseCase useCase;
    private final AnalyticsResultPort<SalaryAnalyticsResponse> presenter;

    public AnalyticsGrpcService(SalaryAnalyticsUseCase useCase,
                                @Qualifier("grpcAnalyticsPresenter") AnalyticsResultPort<SalaryAnalyticsResponse> presenter) {
        this.useCase = useCase;
        this.presenter = presenter;
    }

    @Override
    public void calculate(AnalyticsRequest request, StreamObserver<SalaryAnalyticsResponse> responseObserver) {
        try {
            AnalyticsQuery query = new AnalyticsQuery(
                    StringUtils.hasText(request.getCity()) ? request.getCity() : null,
                    mapSource(request.getSource())
            );
            SalaryAnalyticsResponse response = useCase.calculate(query, presenter);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid gRPC request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to handle gRPC request", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Unexpected error")
                    .augmentDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    private VacancySource mapSource(VacancySourceProto proto) {
        return switch (proto) {
            case HH -> VacancySource.HH;
            case SUPERJOB -> VacancySource.SUPERJOB;
            case HABR -> VacancySource.HABR;
            case RABOTA -> VacancySource.RABOTA;
            case SOURCE_UNSPECIFIED, UNRECOGNIZED -> null;
        };
    }
}
