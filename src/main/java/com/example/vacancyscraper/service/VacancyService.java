package com.example.vacancyscraper.service;

import com.example.vacancyscraper.model.ParsingStats;
import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.model.VacancySource;
import com.example.vacancyscraper.metrics.ParsingMetrics;
import com.example.vacancyscraper.parser.VacancySiteParser;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VacancyService {

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);

    private final List<VacancySiteParser> parsers;
    private final VacancyPersistenceService vacancyPersistenceService;
    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final String userAgent;
    private final ParsingMetrics parsingMetrics;
    private final Semaphore requestLimiter = new Semaphore(8); // ограничиваем параллельные HTTP-запросы
    private final Tracer tracer;

    private final AtomicInteger successCounter = new AtomicInteger();
    private final AtomicInteger errorCounter = new AtomicInteger();
    private final AtomicReference<Instant> lastBatchStarted = new AtomicReference<>();
    private final AtomicLong lastBatchDurationMs = new AtomicLong();

    public VacancyService(List<VacancySiteParser> parsers,
                          VacancyPersistenceService vacancyPersistenceService,
                          ExecutorService executorService,
                          HttpClient httpClient,
                          @Value("${vacancy.user-agent}") String userAgent,
                          ParsingMetrics parsingMetrics,
                          Tracer tracer) {
        this.parsers = parsers;
        this.vacancyPersistenceService = vacancyPersistenceService;
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.userAgent = userAgent;
        this.parsingMetrics = parsingMetrics;
        this.tracer = tracer;
    }

    public Vacancy parseSingle(String url) {
        var timerSample = parsingMetrics.startSingle();
        VacancySiteParser parser = resolveParser(url);
        log.info("Starting single parse for url={} using parser={}", url, parser.getClass().getSimpleName());
        FutureTask<Vacancy> task = new FutureTask<>(() -> fetchAndParse(url, parser));
        Thread thread = new Thread(task, parser.getClass().getSimpleName() + "-thread");
        thread.start();
        try {
            Vacancy vacancy = task.get();
            Vacancy saved = vacancyPersistenceService.saveWithLock(vacancy);
            successCounter.incrementAndGet();
            parsingMetrics.incrementSuccess();
            parsingMetrics.recordSingle(timerSample);
            log.info("Parsed and saved vacancy url={}, title='{}', source={}, id={}", url, saved.getTitle(), saved.getSource(), saved.getId());
            return saved;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCounter.incrementAndGet();
            parsingMetrics.incrementError();
            parsingMetrics.recordSingle(timerSample);
            throw new IllegalStateException("Parsing interrupted", e);
        } catch (ExecutionException e) {
            errorCounter.incrementAndGet();
            parsingMetrics.incrementError();
            parsingMetrics.recordSingle(timerSample);
            throw new IllegalStateException("Failed to parse " + url, e);
        }
    }

    public List<Vacancy> parseBatch(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        var timerSample = parsingMetrics.startBatch();
        Instant start = Instant.now();
        lastBatchStarted.set(start);
        log.info("Starting batch parse for {} urls", urls.size());

        List<Callable<Vacancy>> tasks = urls.stream()
                .filter(Objects::nonNull)
                .distinct() // не дергаем одно и то же несколько раз
                .map(url -> (Callable<Vacancy>) () -> fetchAndParse(url, resolveParser(url)))
                .collect(Collectors.toList());

        List<Vacancy> result = new ArrayList<>();
        try {
            List<Future<Vacancy>> futures = executorService.invokeAll(tasks);
            for (Future<Vacancy> future : futures) {
                try {
                    Vacancy vacancy = future.get();
                    Vacancy savedVacancy = vacancyPersistenceService.saveWithLock(vacancy);
                    successCounter.incrementAndGet();
                    parsingMetrics.incrementSuccess();
                    log.info("Saved vacancy from batch url={}, title='{}', source={}", savedVacancy.getUrl(), savedVacancy.getTitle(), savedVacancy.getSource());
                    result.add(savedVacancy);
                } catch (ExecutionException e) {
                    errorCounter.incrementAndGet();
                    parsingMetrics.incrementError();
                    log.warn("Failed to parse vacancy in batch", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCounter.incrementAndGet();
            parsingMetrics.incrementError();
            log.warn("Batch parsing interrupted", e);
        }
        long duration = Duration.between(start, Instant.now()).toMillis();
        lastBatchDurationMs.set(duration);
        parsingMetrics.recordBatch(timerSample);
        log.info("Batch parsed {} vacancies in {} ms", result.size(), duration);
        return result;
    }

    @Cacheable(cacheNames = "vacancies",
            key = "T(java.util.Objects).hash(#city, #source, #sortBy, #parallel)",
            sync = true)
    public List<Vacancy> findAll(String city, VacancySource source, String sortBy, boolean useParallel) {
        long started = System.nanoTime();
        List<Vacancy> vacancies = vacancyPersistenceService.findAll();
        Stream<Vacancy> stream = useParallel ? vacancies.parallelStream() : vacancies.stream();

        Comparator<Vacancy> comparator = buildComparator(sortBy);
        List<Vacancy> result = stream
                .filter(v -> city == null || (v.getCity() != null && v.getCity().toLowerCase().contains(city.toLowerCase())))
                .filter(v -> source == null || v.getSource() == source)
                .sorted(comparator)
                .collect(Collectors.toList());

        long durationMs = (System.nanoTime() - started) / 1_000_000;
        log.info("Fetched {} vacancies with filters city='{}', source={}, sortBy={}, durationMs={}", result.size(), city, source, sortBy, durationMs);
        return result;
    }

    public ParsingStats getStats() {
        long totalParsed = vacancyPersistenceService.count();
        long totalErrors = errorCounter.get();
        return new ParsingStats(totalParsed, totalErrors, lastBatchStarted.get(), lastBatchDurationMs.get());
    }

    private Comparator<Vacancy> buildComparator(String sortBy) {
        if (sortBy == null) {
            return Comparator.comparing(v -> v.getTitle() == null ? "" : v.getTitle(), String.CASE_INSENSITIVE_ORDER);
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
        Span span = tracer.spanBuilder("vacancy.fetchAndParse")
                .setAttribute("vacancy.url", url)
                .setAttribute("parser", parser.getClass().getSimpleName())
                .startSpan();
        try {
            requestLimiter.acquire();
            span.addEvent("http.send");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            log.info("Sending request to url={} with parser={}", url, parser.getClass().getSimpleName());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            span.setAttribute("http.status_code", response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String html = response.body();
                log.info("Received response status={} contentLength={} for url={}", response.statusCode(), html.length(), url);
                span.addEvent("parse.html");
                Vacancy vacancy = parser.parse(url, html);
                vacancy.setUrl(url);
                log.info("Parsed vacancy url={}, title='{}', source={}", url, vacancy.getTitle(), vacancy.getSource());
                span.setStatus(StatusCode.OK);
                return vacancy;
            }
            throw new IllegalStateException("Bad response code: " + response.statusCode());
        } catch (Exception e) {
            parsingMetrics.incrementError();
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw new IllegalStateException("Failed to fetch url: " + url, e);
        } finally {
            requestLimiter.release();
            span.end();
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
