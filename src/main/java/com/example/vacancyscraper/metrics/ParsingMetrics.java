package com.example.vacancyscraper.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ParsingMetrics {

    private final Timer singleParseTimer;
    private final Timer batchParseTimer;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter savedCounter;

    public ParsingMetrics(MeterRegistry meterRegistry) {
        this.singleParseTimer = Timer.builder("vacancy.parsing.time")
                .description("Time spent parsing a single vacancy")
                .tag("type", "single")
                .register(meterRegistry);

        this.batchParseTimer = Timer.builder("vacancy.parsing.time")
                .description("Time spent parsing a batch of vacancies")
                .tag("type", "batch")
                .register(meterRegistry);

        this.successCounter = Counter.builder("vacancy.parsing.success")
                .description("Number of successfully parsed vacancies")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("vacancy.parsing.error")
                .description("Number of failed vacancy parsing attempts")
                .register(meterRegistry);

        this.savedCounter = Counter.builder("vacancy.persistence.saved")
                .description("Number of vacancies saved to the database")
                .register(meterRegistry);
    }

    public Timer.Sample startSingle() {
        return Timer.start();
    }

    public Timer.Sample startBatch() {
        return Timer.start();
    }

    public void recordSingle(Timer.Sample sample) {
        sample.stop(singleParseTimer);
    }

    public void recordBatch(Timer.Sample sample) {
        sample.stop(batchParseTimer);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

    public void incrementSaved() {
        savedCounter.increment();
    }
}
