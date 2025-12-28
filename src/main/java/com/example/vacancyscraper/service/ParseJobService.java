package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.ParseJob;
import com.example.vacancyscraper.model.ParseJobStatus;
import com.example.vacancyscraper.model.Vacancy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Service
public class ParseJobService {

    private static final Logger log = LoggerFactory.getLogger(ParseJobService.class);

    private final ExecutorService executorService;
    private final VacancyService vacancyService;
    private final long defaultDelayMs;
    private final Map<String, ParseJob> jobs = new ConcurrentHashMap<>();
    private final List<Consumer<ParseJob>> listeners = new CopyOnWriteArrayList<>();

    public ParseJobService(ExecutorService executorService,
                           VacancyService vacancyService,
                           @Value("${vacancy.frontend.default-delay-ms:0}") long defaultDelayMs) {
        this.executorService = executorService;
        this.vacancyService = vacancyService;
        this.defaultDelayMs = Math.max(0, defaultDelayMs);
    }

    public ParseJob startJob(String url, Long delayMs) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        long effectiveDelay = delayMs != null ? Math.max(delayMs, 0) : defaultDelayMs;
        String jobId = UUID.randomUUID().toString();
        ParseJob job = new ParseJob(jobId, url, effectiveDelay, Instant.now());
        jobs.put(jobId, job);
        executorService.submit(() -> executeJob(job));
        log.info("Scheduled parse job id={} url={} delayMs={}", jobId, url, effectiveDelay);
        return job;
    }

    public ParseJob getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId))
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    public void registerListener(Consumer<ParseJob> listener) {
        listeners.add(listener);
    }

    private void executeJob(ParseJob job) {
        job.setStatus(ParseJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        try {
            if (job.getArtificialDelayMs() > 0) {
                Thread.sleep(job.getArtificialDelayMs());
            }
            Vacancy vacancy = vacancyService.parseSingle(job.getUrl());
            job.setVacancy(vacancy);
            job.setStatus(ParseJobStatus.SUCCESS);
            log.info("Job {} finished successfully", job.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.setStatus(ParseJobStatus.ERROR);
            job.setError("Job interrupted");
            log.warn("Job {} interrupted", job.getId());
        } catch (Exception e) {
            job.setStatus(ParseJobStatus.ERROR);
            job.setError(e.getMessage());
            log.warn("Job {} failed: {}", job.getId(), e.getMessage());
        } finally {
            job.setFinishedAt(Instant.now());
            notifyListeners(job);
        }
    }

    private void notifyListeners(ParseJob job) {
        listeners.forEach(listener -> {
            try {
                listener.accept(job);
            } catch (Exception e) {
                log.debug("Listener failed for job {}: {}", job.getId(), e.getMessage());
            }
        });
    }
}
