package com.example.vacancyscraper.benchmark;

import com.example.vacancyscraper.model.Vacancy;
import com.example.vacancyscraper.parser.HhVacancyParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * эмуляция реального парсера
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class VacancyParserBenchmark {

    @State(Scope.Benchmark)
    public static class DataState {
        HhVacancyParser parser;
        String html;
        List<String> urls;
        Map<String, Vacancy> store;

        @Setup
        public void setup() throws Exception {
            parser = new HhVacancyParser();
            var resource = VacancyParserBenchmark.class.getClassLoader().getResource("fixtures/hh_vacancy.html");
            if (resource == null) {
                throw new IllegalStateException("Fixture fixtures/hh_vacancy.html not found");
            }
            html = Files.readString(Paths.get(resource.toURI()));
            urls = Collections.nCopies(200, "https://hh.ru/vacancy/128763453");
            store = new HashMap<>();
        }
    }

    @Benchmark
    public Vacancy parseSingle(DataState state) {
        return state.parser.parse(state.urls.get(0), state.html);
    }

    @Benchmark
    public int parseBatchParallel(DataState state) {
        return state.urls
                .parallelStream()
                .map(url -> state.parser.parse(url, state.html))
                .mapToInt(vacancy -> vacancy.getTitle().length())
                .sum();
    }

    /**
     * End-to-end: 
     */
    @Benchmark
    public int endToEndWithStore(DataState state) {
        int sum = 0;
        for (String url : state.urls) {
            Vacancy vacancy = state.parser.parse(url, state.html);
            state.store.put(url, vacancy);
            sum += vacancy.getTitle() != null ? vacancy.getTitle().length() : 0;
        }
        return sum;
    }
}
