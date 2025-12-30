package com.example.vacancyscraper.benchmark;

import com.example.vacancyscraper.analytics.SalaryParser;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Сравнение способов разбора зарплатных строк: for-loop vs stream vs parallelStream.

 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
public class ParseBenchmark {

    @State(Scope.Benchmark)
    public static class DataState {
        List<String> salaryStrings;

        @Setup
        public void setup() {
            salaryStrings = new ArrayList<>();
            salaryStrings.add("от 130 000 до 150 000 ₽");
            salaryStrings.add("120 000-180 000");
            salaryStrings.add("до 200 000 руб.");
            salaryStrings.add("150000");
            salaryStrings.add("не указано");
            List<String> base = new ArrayList<>(salaryStrings);
            for (int i = 0; i < 200; i++) {
                salaryStrings.addAll(base);
            }
        }
    }

    @Benchmark
    public int parseWithForLoop(DataState state) {
        int sum = 0;
        List<String> data = state.salaryStrings;
        for (int i = 0; i < data.size(); i++) {
            SalaryParser.parse(data.get(i));
            sum += i;
        }
        return sum;
    }

    @Benchmark
    public int parseWithStream(DataState state) {
        return state.salaryStrings.stream()
                .map(SalaryParser::parse)
                .mapToInt(range -> range.isEmpty() ? 0 : range.from() == null ? 0 : range.from())
                .sum();
    }

    @Benchmark
    public int parseWithParallelStream(DataState state) {
        return state.salaryStrings.parallelStream()
                .map(SalaryParser::parse)
                .mapToInt(range -> range.isEmpty() ? 0 : range.from() == null ? 0 : range.from())
                .sum();
    }
}
