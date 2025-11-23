package com.example.vacancyscraper.model;

import java.time.Instant;

public class ParsingStats {
    private int totalParsed;
    private int totalErrors;
    private Instant lastBatchStartedAt;
    private long lastBatchDurationMs;

    public ParsingStats() {
    }

    public ParsingStats(int totalParsed, int totalErrors, Instant lastBatchStartedAt, long lastBatchDurationMs) {
        this.totalParsed = totalParsed;
        this.totalErrors = totalErrors;
        this.lastBatchStartedAt = lastBatchStartedAt;
        this.lastBatchDurationMs = lastBatchDurationMs;
    }

    public int getTotalParsed() {
        return totalParsed;
    }

    public void setTotalParsed(int totalParsed) {
        this.totalParsed = totalParsed;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(int totalErrors) {
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
