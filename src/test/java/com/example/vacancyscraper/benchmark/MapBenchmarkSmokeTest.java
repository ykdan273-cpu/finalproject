package com.example.vacancyscraper.benchmark;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


class MapBenchmarkSmokeTest {

    @Test
    void runJmhSmoke() throws Exception {
        Options opt = new OptionsBuilder()
                .include(MapBenchmark.class.getSimpleName())
                .warmupIterations(0)
                .measurementIterations(1)
                .measurementTime(TimeValue.milliseconds(500))
                .threads(1)
                .forks(0)
                .shouldDoGC(true)
                .build();
        new Runner(opt).run();
    }
}
