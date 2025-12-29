package com.example.vacancyscraper.analytics.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;

@Configuration
public class GrpcServerConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);

    private final int port;
    private Server server;

    public GrpcServerConfig(@Value("${vacancy.grpc.port:6565}") int port) {
        this.port = port;
    }

    @Bean
    public Server grpcServer(List<BindableService> services, GrpcLoggingInterceptor loggingInterceptor) throws IOException {
        ServerBuilder<?> builder = ServerBuilder.forPort(port);
        services.forEach(service -> builder.addService(ServerInterceptors.intercept(service, loggingInterceptor)));
        server = builder.build().start();
        log.info("gRPC server started on port {}", port);
        return server;
    }

    @PreDestroy
    public void shutdown() {
        if (server != null) {
            server.shutdown();
            log.info("gRPC server on port {} stopped", port);
        }
    }
}
