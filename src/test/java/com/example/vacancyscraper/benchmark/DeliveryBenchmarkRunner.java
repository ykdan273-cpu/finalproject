package com.example.vacancyscraper.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class DeliveryBenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(DeliveryBenchmark.class.getSimpleName())
                .forks(0) // keep in-VM for mvn exec:java convenience
                .build();
        new Runner(options).run();
    }
}
