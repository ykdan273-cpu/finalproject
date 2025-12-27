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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Сравнение ConcurrentHashMap и Collections.synchronizedMap в стиле примера (AverageTime + готовые стейты).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Threads(8)
public class MapBenchmark {

    private static final int MAP_SIZE = 100_000;

    @State(Scope.Benchmark)
    public static class ConcurrentMapState {
        Map<String, Integer> map;

        @Setup
        public void setup() {
            map = new ConcurrentHashMap<>();
            for (int i = 0; i < MAP_SIZE; i++) {
                map.put("key" + i, i);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class SynchronizedMapState {
        Map<String, Integer> map;

        @Setup
        public void setup() {
            Map<String, Integer> raw = new HashMap<>();
            for (int i = 0; i < MAP_SIZE; i++) {
                raw.put("key" + i, i);
            }
            map = Collections.synchronizedMap(raw);
        }
    }

    @Benchmark
    public Integer putConcurrent(ConcurrentMapState state) {
        int key = ThreadLocalRandom.current().nextInt(MAP_SIZE);
        int value = ThreadLocalRandom.current().nextInt();
        state.map.put("key" + key, value);
        return state.map.get("key" + key);
    }

    @Benchmark
    public Integer putSynchronized(SynchronizedMapState state) {
        int key = ThreadLocalRandom.current().nextInt(MAP_SIZE);
        int value = ThreadLocalRandom.current().nextInt();
        state.map.put("key" + key, value);
        return state.map.get("key" + key);
    }
}
