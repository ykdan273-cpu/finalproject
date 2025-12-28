package com.example.vacancyscraper.dto;

import com.example.vacancyscraper.model.ParseJob;
import com.example.vacancyscraper.model.ParseJobStatus;
import com.example.vacancyscraper.model.Vacancy;

import java.time.Instant;

public class ParseJobResponse {

    private String jobId;
    private ParseJobStatus status;
    private String url;
    private long delayMs;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private long durationMs;
    private String error;
    private Vacancy vacancy;

    public static ParseJobResponse from(ParseJob job) {
        ParseJobResponse response = new ParseJobResponse();
        response.jobId = job.getId();
        response.status = job.getStatus();
        response.url = job.getUrl();
        response.delayMs = job.getArtificialDelayMs();
        response.requestedAt = job.getRequestedAt();
        response.startedAt = job.getStartedAt();
        response.finishedAt = job.getFinishedAt();
        response.durationMs = job.getDurationMs();
        response.error = job.getError();
        if (job.isFinished()) {
            response.vacancy = job.getVacancy();
        }
        return response;
    }

    public String getJobId() {
        return jobId;
    }

    public ParseJobStatus getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getError() {
        return error;
    }

    public Vacancy getVacancy() {
        return vacancy;
    }
}
