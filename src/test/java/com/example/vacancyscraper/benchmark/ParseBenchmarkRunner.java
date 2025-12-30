package com.example.vacancyscraper.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ParseBenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ParseBenchmark.class.getSimpleName())
                .forks(0) 
                .build();
        new Runner(options).run();
    }
}
