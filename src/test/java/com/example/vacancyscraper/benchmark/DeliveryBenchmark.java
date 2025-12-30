package com.example.vacancyscraper.benchmark;

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

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Compare two result delivery models for a finished parsing job:
 *  - REST polling every small interval (scaled down from 500 ms to keep benchmark short)
 *  - WebSocket push (modeled as a blocking queue handoff)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(0)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
public class DeliveryBenchmark {

    @State(Scope.Thread)
    public static class JobState {
        
        private static final Duration PROCESSING_DELAY = Duration.ofMillis(50);
        private static final Duration POLL_INTERVAL = Duration.ofMillis(5);

        ExecutorService executor;

        @Setup
        public void setup() {
            executor = Executors.newVirtualThreadPerTaskExecutor();
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        CompletableFuture<String> startJob() {
            return CompletableFuture.supplyAsync(() -> {
                sleep(PROCESSING_DELAY);
                return "done";
            }, executor);
        }

        private void sleep(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @Benchmark
    public int restPolling(JobState state) {
        CompletableFuture<String> job = state.startJob();
        while (true) {
            if (job.isDone()) {
                return job.join().length();
            }
            sleep(JobState.POLL_INTERVAL);
        }
    }

    @Benchmark
    public int webSocketPush(JobState state) throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
        state.startJob().thenAccept(queue::offer);
        String result = queue.take();
        return result.length();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
