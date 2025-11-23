package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.ParsingStats;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.example.vacancyscraper.parser.VacancySiteParser;
import com.example.vacancyscraper.storage.VacancyFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VacancyService {

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);

    private final List<VacancySiteParser> parsers;
    private final VacancyFileRepository repository;
    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final String userAgent;

    // безопасные потоки(атомарные величины)
    private final ConcurrentLinkedQueue<String> errorMessages = new ConcurrentLinkedQueue<>();
    private final AtomicInteger successCounter = new AtomicInteger();
    private final AtomicInteger errorCounter = new AtomicInteger();
    private final AtomicReference<Instant> lastBatchStarted = new AtomicReference<>();
    private final AtomicLong lastBatchDurationMs = new AtomicLong();

    public VacancyService(List<VacancySiteParser> parsers,
                          VacancyFileRepository repository,
                          ExecutorService executorService,
                          HttpClient httpClient,
                          @Value("${vacancy.user-agent}") String userAgent) {
        this.parsers = parsers;
        this.repository = repository;
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.userAgent = userAgent;
    }

    public Vacancy parseSingle(String url) {
        VacancySiteParser parser = resolveParser(url);
        FutureTask<Vacancy> task = new FutureTask<>(() -> fetchAndParse(url, parser));
        Thread thread = new Thread(task, parser.getClass().getSimpleName() + "-thread");
        thread.start();
        try {
            Vacancy vacancy = task.get();
            repository.save(vacancy);
            successCounter.incrementAndGet();
            return vacancy;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCounter.incrementAndGet();
            errorMessages.add("Interrupted: " + url);
            throw new IllegalStateException("Parsing interrupted", e);
        } catch (ExecutionException e) {
            errorCounter.incrementAndGet();
            errorMessages.add(e.getMessage());
            throw new IllegalStateException("Failed to parse " + url, e);
        }
    }

    public List<Vacancy> parseBatch(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        Instant start = Instant.now();
        lastBatchStarted.set(start);

        List<Callable<Vacancy>> tasks = urls.stream()
                .map(url -> (Callable<Vacancy>) () -> fetchAndParse(url, resolveParser(url)))
                .collect(Collectors.toList());

        List<Vacancy> result = new ArrayList<>();
        try {
            List<Future<Vacancy>> futures = executorService.invokeAll(tasks);
            for (Future<Vacancy> future : futures) {
                try {
                    Vacancy vacancy = future.get();
                    repository.save(vacancy);
                    successCounter.incrementAndGet();
                    result.add(vacancy);
                } catch (ExecutionException e) {
                    errorCounter.incrementAndGet();
                    errorMessages.add(e.getMessage());
                    log.warn("Failed to parse vacancy in batch", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCounter.incrementAndGet();
            errorMessages.add("Batch interrupted");
        }
        long duration = Duration.between(start, Instant.now()).toMillis();
        lastBatchDurationMs.set(duration);
        log.info("Batch parsed {} vacancies in {} ms", result.size(), duration);
        return result;
    }

    public List<Vacancy> findAll(String city, VacancySource source, String sortBy, boolean useParallel) {
        List<Vacancy> vacancies = repository.findAll();
        long started = System.nanoTime();
        Stream<Vacancy> stream = useParallel ? vacancies.parallelStream() : vacancies.stream();

        Stream<Vacancy> filtered = stream
                .filter(v -> city == null || (v.getCity() != null && v.getCity().toLowerCase().contains(city.toLowerCase())))
                .filter(v -> source == null || v.getSource() == source);

        Comparator<Vacancy> comparator = buildComparator(sortBy);
        List<Vacancy> result = filtered
                .sorted(comparator)
                .collect(Collectors.toList());

        long durationMs = (System.nanoTime() - started) / 1_000_000;
        log.info("Filtered {} vacancies using {} stream in {} ms", result.size(), useParallel ? "parallel" : "sequential", durationMs);
        return result;
    }

    public ParsingStats getStats() {
        return new ParsingStats(
                successCounter.get(),
                errorCounter.get(),
                lastBatchStarted.get(),
                lastBatchDurationMs.get()
        );
    }

    public List<String> getErrors() {
        return new ArrayList<>(errorMessages);
    }

    private Comparator<Vacancy> buildComparator(String sortBy) {
        if (sortBy == null) {
            return Comparator.comparing(v -> Optional.ofNullable(v.getTitle()).orElse(""));
        }
        switch (sortBy.toLowerCase()) {
            case "date":
                return Comparator.comparing(Vacancy::getPublishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder()));
            case "salary":
                return Comparator.comparing(Vacancy::getSalary,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "title":
            default:
                return Comparator.comparing(Vacancy::getTitle,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
    }

    private Vacancy fetchAndParse(String url, VacancySiteParser parser) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String html = response.body();
                Vacancy vacancy = parser.parse(url, html);
                vacancy.setUrl(url);
                return vacancy;
            }
            throw new IllegalStateException("Bad response code: " + response.statusCode());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch url: " + url, e);
        }
    }

    private VacancySiteParser resolveParser(String url) {
        Objects.requireNonNull(url, "URL must not be null");
        return parsers.stream()
                .filter(p -> p.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No parser found for url " + url));
    }
}
