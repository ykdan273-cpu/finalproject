package com.example.vacancyscraper.model;

import java.time.Instant;

public class ParsingStats {
    private long totalParsed;
    private long totalErrors;
    private Instant lastBatchStartedAt;
    private long lastBatchDurationMs;

    public ParsingStats() {
    }

    public ParsingStats(long totalParsed, long totalErrors, Instant lastBatchStartedAt, long lastBatchDurationMs) {
        this.totalParsed = totalParsed;
        this.totalErrors = totalErrors;
        this.lastBatchStartedAt = lastBatchStartedAt;
        this.lastBatchDurationMs = lastBatchDurationMs;
    }

    public long getTotalParsed() {
        return totalParsed;
    }

    public void setTotalParsed(long totalParsed) {
        this.totalParsed = totalParsed;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(long totalErrors) {
        this.totalErrors = totalErrors;
    }

    public Instant getLastBatchStartedAt() {
        return lastBatchStartedAt;
    }

    public void setLastBatchStartedAt(Instant lastBatchStartedAt) {
        this.lastBatchStartedAt = lastBatchStartedAt;
    }

    public long getLastBatchDurationMs() {
        return lastBatchDurationMs;
    }

    public void setLastBatchDurationMs(long lastBatchDurationMs) {
        this.lastBatchDurationMs = lastBatchDurationMs;
    }
}
