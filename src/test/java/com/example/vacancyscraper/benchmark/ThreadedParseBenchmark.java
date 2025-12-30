package com.example.vacancyscraper.benchmark;

import com.example.vacancyscraper.analytics.SalaryParser;
import com.example.vacancyscraper.analytics.SalaryRange;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

@Fork(value = 0)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
public class ThreadedParseBenchmark {

    @State(Scope.Benchmark)
    public static class ExecState {
        List<String> salaryStrings;
        ExecutorService platformPool;
        ExecutorService virtualPool;

        @Setup
        public void setup() {
            List<String> base = List.of(
                    "130 000 to 150 000",
                    "120000-180000",
                    "up to 200 000",
                    "150000",
                    "not specified"
            );
            salaryStrings = new ArrayList<>(base);
            for (int i = 0; i < 200; i++) {
                salaryStrings.addAll(base);
            }
            platformPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            virtualPool = Executors.newVirtualThreadPerTaskExecutor();
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            platformPool.shutdown();
            virtualPool.shutdown();
            platformPool.awaitTermination(5, TimeUnit.SECONDS);
            virtualPool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Benchmark
    public int platformThreads(ExecState state) throws Exception {
        return parseWithExecutor(state.platformPool, state.salaryStrings);
    }

    @Benchmark
    public int virtualThreads(ExecState state) throws Exception {
        return parseWithExecutor(state.virtualPool, state.salaryStrings);
    }

    private int parseWithExecutor(ExecutorService executor, List<String> data) throws Exception {
        List<Future<Integer>> futures = new ArrayList<>(data.size());
        for (String text : data) {
            futures.add(executor.submit(() -> toInt(SalaryParser.parse(text))));
        }
        int sum = 0;
        for (Future<Integer> future : futures) {
            sum += future.get();
        }
        return sum;
    }

    private int toInt(SalaryRange range) {
        if (range == null || range.isEmpty() || range.from() == null) {
            return 0;
        }
        return range.from();
    }
}
