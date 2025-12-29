package com.example.vacancyscraper.analytics.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GrpcLoggingInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcLoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        log.info("gRPC call started: {}", method);
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                log.info("gRPC call finished: {} status={}", method, status);
                super.close(status, trailers);
            }
        }, headers);
    }
}
