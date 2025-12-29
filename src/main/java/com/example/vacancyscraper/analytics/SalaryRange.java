package com.example.vacancyscraper.analytics;

public record SalaryRange(Integer from, Integer to) {
    public boolean isEmpty() {
        return from == null && to == null;
    }
}
