package com.example.vacancyscraper.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class ExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Bean
    public ExecutorService executorService() {
        return executorService;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @PostConstruct
    public void startExecutorLogger() {
        Thread loggerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                log.info("Executor stats: pool (virtual threads) running");
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "executor-logger");
        loggerThread.setDaemon(true);
        loggerThread.start();
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ThreadFactory daemonFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + thread.getId());
            thread.setDaemon(true);
            return thread;
        };
    }
}
