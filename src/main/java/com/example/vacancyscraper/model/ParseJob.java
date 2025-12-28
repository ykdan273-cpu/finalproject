package com.example.vacancyscraper.model;

import java.time.Duration;
import java.time.Instant;

public class ParseJob {
    private final String id;
    private final String url;
    private final long artificialDelayMs;
    private final Instant requestedAt;

    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile ParseJobStatus status;
    private volatile Vacancy vacancy;
    private volatile String error;

    public ParseJob(String id, String url, long artificialDelayMs, Instant requestedAt) {
        this.id = id;
        this.url = url;
        this.artificialDelayMs = artificialDelayMs;
        this.requestedAt = requestedAt;
        this.status = ParseJobStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public long getArtificialDelayMs() {
        return artificialDelayMs;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public ParseJobStatus getStatus() {
        return status;
    }

    public void setStatus(ParseJobStatus status) {
        this.status = status;
    }

    public Vacancy getVacancy() {
        return vacancy;
    }

    public void setVacancy(Vacancy vacancy) {
        this.vacancy = vacancy;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isFinished() {
        return status == ParseJobStatus.SUCCESS || status == ParseJobStatus.ERROR;
    }

    public long getDurationMs() {
        if (startedAt == null) {
            return 0;
        }
        Instant end = finishedAt != null ? finishedAt : Instant.now();
        return Duration.between(startedAt, end).toMillis();
    }
}
